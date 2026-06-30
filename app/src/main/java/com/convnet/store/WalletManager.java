package com.convnet.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 钱包管理 — 本地私钥签名 + Polygon RPC 广播
 * 支持: 导入私钥、签名交易、查询余额
 */
public class WalletManager {

    private static final String TAG = "WalletManager";
    private static final String PREFS_NAME = "convnet_wallet";
    private static final String KEY_ADDRESS = "wallet_address";
    private static final String KEY_PRIVATE = "wallet_private_key";
    private static WalletManager instance;
    private final SharedPreferences prefs;
    private final OkHttpClient client;

    // secp256k1 曲线参数
    private static final BigInteger N = new BigInteger(
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    private static final BigInteger P = new BigInteger(
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    private static final BigInteger A = BigInteger.ZERO;
    private static final BigInteger B = BigInteger.valueOf(7);
    private static final BigInteger Gx = new BigInteger(
        "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    private static final BigInteger Gy = new BigInteger(
        "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);

    private WalletManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public static synchronized WalletManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new WalletManager(ctx);
        }
        return instance;
    }

    public boolean hasWallet() {
        return prefs.contains(KEY_ADDRESS);
    }

    public String getAddress() {
        return prefs.getString(KEY_ADDRESS, null);
    }

    public String getPrivateKey() {
        return prefs.getString(KEY_PRIVATE, null);
    }

    public void saveWallet(String address, String privateKey) {
        prefs.edit()
                .putString(KEY_ADDRESS, address)
                .putString(KEY_PRIVATE, privateKey)
                .apply();
    }

    public void clearWallet() {
        prefs.edit().clear().apply();
    }

    /**
     * 从私钥推导以太坊地址
     */
    public static String addressFromPrivateKey(String privateKeyHex) {
        try {
            if (privateKeyHex.startsWith("0x")) privateKeyHex = privateKeyHex.substring(2);
            BigInteger privKey = new BigInteger(privateKeyHex, 16);

            ECCurve curve = new ECCurve.Fp(P, A, B);
            ECPoint g = curve.createPoint(Gx, Gy);
            ECDomainParameters domain = new ECDomainParameters(curve, g, N);

            ECPoint q = g.multiply(privKey).normalize();
            byte[] pubBytes = q.getEncoded(false); // uncompressed

            // Keccak-256 hash of pubkey (skip first byte)
            KeccakDigest digest = new KeccakDigest(256);
            digest.update(pubBytes, 1, pubBytes.length - 1);
            byte[] hash = new byte[32];
            digest.doFinal(hash, 0);

            // Take last 20 bytes
            StringBuilder addr = new StringBuilder("0x");
            for (int i = 12; i < 32; i++) {
                addr.append(String.format("%02x", hash[i]));
            }
            return addr.toString();
        } catch (Exception e) {
            Log.e(TAG, "addressFromPrivateKey error", e);
            return null;
        }
    }

    /**
     * 发送链上交易
     * @param to 合约地址
     * @param value ETH value (BigInteger)
     * @param data 合约调用数据 (hex string with 0x prefix)
     * @param callback (success, txHashOrError)
     */
    public void sendTransaction(String to, BigInteger value, String data,
                                 TxCallback callback) {
        new Thread(() -> {
            try {
                String from = getAddress();
                String privKey = getPrivateKey();
                if (from == null || privKey == null) {
                    callback.onResult(false, "钱包未连接");
                    return;
                }

                // 1. Get nonce
                JSONArray nonceParams = new JSONArray().put(from).put("latest");
                String nonceHex = rpcCall("eth_getTransactionCount", nonceParams);
                BigInteger nonce = new BigInteger(nonceHex.substring(2), 16);

                // 2. Get gas price
                String gasPriceHex = rpcCall("eth_gasPrice", new JSONArray());
                BigInteger gasPrice = new BigInteger(gasPriceHex.substring(2), 16);

                // 3. Estimate gas (simplified: use 200000 for contract calls)
                BigInteger gasLimit = BigInteger.valueOf(300000);

                // 4. Build raw transaction
                String toAddr = to.startsWith("0x") ? to.substring(2) : to;
                String dataHex = data.startsWith("0x") ? data.substring(2) : data;

                // RLP encode
                byte[] rawTx = encodeRawTransaction(
                    nonce, gasPrice, gasLimit, toAddr, value, dataHex,
                    BigInteger.valueOf(ContractConfig.CHAIN_ID)
                );

                // 5. Sign
                byte[] signedTx = signTransaction(rawTx, privKey);

                // 6. Broadcast
                String txHex = "0x" + bytesToHex(signedTx);
                JSONArray sendParams = new JSONArray().put(txHex);
                String txHash = rpcCall("eth_sendRawTransaction", sendParams);

                callback.onResult(true, txHash);
            } catch (Exception e) {
                Log.e(TAG, "sendTransaction error", e);
                callback.onResult(false, e.getMessage());
            }
        }).start();
    }

    public interface TxCallback {
        void onResult(boolean success, String txHashOrError);
    }

    // ===== RLP Encoding =====

    private byte[] encodeRawTransaction(BigInteger nonce, BigInteger gasPrice,
            BigInteger gasLimit, String to, BigInteger value, String data,
            BigInteger chainId) {
        // RLP encode: [nonce, gasPrice, gasLimit, to, value, data, chainId, 0, 0]
        byte[][] elements = new byte[9][];
        elements[0] = toRLP(nonce);
        elements[1] = toRLP(gasPrice);
        elements[2] = toRLP(gasLimit);
        elements[3] = hexToBytes(to);
        elements[4] = toRLP(value);
        elements[5] = hexToBytes(data);
        elements[6] = toRLP(chainId);
        elements[7] = new byte[0];
        elements[8] = new byte[0];
        return rlpEncodeList(elements);
    }

    private byte[] signTransaction(byte[] rawTx, String privateKeyHex) throws Exception {
        if (privateKeyHex.startsWith("0x")) privateKeyHex = privateKeyHex.substring(2);
        BigInteger privKey = new BigInteger(privateKeyHex, 16);

        // Keccak-256 hash
        KeccakDigest digest = new KeccakDigest(256);
        digest.update(rawTx, 0, rawTx.length);
        byte[] hash = new byte[32];
        digest.doFinal(hash, 0);

        // Sign with secp256k1
        ECCurve curve = new ECCurve.Fp(P, A, B);
        ECPoint g = curve.createPoint(Gx, Gy);
        ECDomainParameters domain = new ECDomainParameters(curve, g, N);
        ECDSASigner signer = new ECDSASigner();
        ECPrivateKeyParameters privParams = new ECPrivateKeyParameters(privKey, domain);
        signer.init(true, privParams);
        BigInteger[] sig = signer.generateSignature(hash);

        BigInteger r = sig[0];
        BigInteger s = sig[1];
        // Low-s normalization
        if (s.compareTo(N.divide(BigInteger.TWO)) > 0) {
            s = N.subtract(s);
        }

        // Get public key for recovery id
        ECPoint q = g.multiply(privKey).normalize();
        ECPublicKeyParameters pubParams = new ECPublicKeyParameters(q, domain);

        // Recovery id (0 or 1)
        int recId = -1;
        for (int i = 0; i < 2; i++) {
            try {
                ECPoint recovered = recoverPublicKey(hash, r, s, i, curve, g, domain);
                if (recovered != null && recovered.equals(q)) {
                    recId = i;
                    break;
                }
            } catch (Exception ignored) {}
        }
        if (recId == -1) recId = 0;

        // Chain ID recovery
        int v = recId + 27 + 8 + ContractConfig.CHAIN_ID * 2;

        // Build signed transaction: [nonce, gasPrice, gasLimit, to, value, data, v, r, s]
        // Re-parse original
        // Simplified: rebuild with signature
        byte[][] elements = new byte[9][];
        // Re-encode from rawTx (parse RLP)
        Object parsed = rlpDecode(rawTx);
        @SuppressWarnings("unchecked")
        java.util.List<byte[]> items = (java.util.List<byte[]>) parsed;
        elements[0] = items.get(0);
        elements[1] = items.get(1);
        elements[2] = items.get(2);
        elements[3] = items.get(3);
        elements[4] = items.get(4);
        elements[5] = items.get(5);
        elements[6] = toRLP(BigInteger.valueOf(v));
        elements[7] = toRLP(r);
        elements[8] = toRLP(s);

        return rlpEncodeList(elements);
    }

    private ECPoint recoverPublicKey(byte[] hash, BigInteger r, BigInteger s, int recId,
            ECCurve curve, ECPoint g, ECDomainParameters domain) {
        try {
            BigInteger n = N;
            BigInteger x = r;
            if (recId >= 2) x = x.add(n);
            if (x.compareTo(P) >= 0) return null;

            // Use curve.createPoint + manual decompression
            ECPoint R = curve.createPoint(x, computeY(x, curve, recId % 2 == 1)).normalize();

            BigInteger e = new BigInteger(1, hash);
            BigInteger rInv = r.modInverse(n);
            BigInteger srInv = s.multiply(rInv).mod(n);
            BigInteger eInv = e.multiply(rInv).mod(n);
            ECPoint q = g.multiply(eInv.negate().mod(n)).add(R.multiply(srInv)).normalize();
            return q;
        } catch (Exception ex) {
            return null;
        }
    }

    private BigInteger computeY(BigInteger x, ECCurve curve, boolean odd) {
        // y² = x³ + 7 (mod P)
        BigInteger x3 = x.modPow(BigInteger.valueOf(3), P);
        BigInteger rhs = x3.add(BigInteger.valueOf(7)).mod(P);
        // Tonelli-Shanks for square root (P ≡ 3 mod 4 for secp256k1)
        BigInteger y = rhs.modPow(P.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), P);
        if (odd != y.testBit(0)) {
            y = P.subtract(y);
        }
        return y;
    }

    // ===== RLP Utilities =====

    private byte[] toRLP(BigInteger value) {
        if (value.equals(BigInteger.ZERO)) return new byte[0];
        byte[] bytes = value.toByteArray();
        // Strip leading zero
        if (bytes[0] == 0 && bytes.length > 1) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return bytes;
    }

    private byte[] hexToBytes(String hex) {
        if (hex.isEmpty()) return new byte[0];
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return data;
    }

    private byte[] rlpEncodeList(byte[][] items) {
        byte[][] encoded = new byte[items.length][];
        int totalLen = 0;
        for (int i = 0; i < items.length; i++) {
            encoded[i] = rlpEncodeItem(items[i]);
            totalLen += encoded[i].length;
        }
        byte[] prefix = encodeLength(totalLen, 0xc0);
        byte[] result = new byte[prefix.length + totalLen];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        int offset = prefix.length;
        for (byte[] e : encoded) {
            System.arraycopy(e, 0, result, offset, e.length);
            offset += e.length;
        }
        return result;
    }

    private byte[] rlpEncodeItem(byte[] data) {
        if (data.length == 1 && (data[0] & 0xFF) < 0x80) {
            return data;
        }
        byte[] prefix = encodeLength(data.length, 0x80);
        byte[] result = new byte[prefix.length + data.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(data, 0, result, prefix.length, data.length);
        return result;
    }

    private byte[] encodeLength(int length, int offset) {
        if (length < 56) {
            return new byte[]{(byte) (offset + length)};
        }
        byte[] lenBytes = toMinimalByteArray(length);
        byte[] result = new byte[1 + lenBytes.length];
        result[0] = (byte) (offset + 55 + lenBytes.length);
        System.arraycopy(lenBytes, 0, result, 1, lenBytes.length);
        return result;
    }

    private byte[] toMinimalByteArray(int value) {
        if (value == 0) return new byte[0];
        int len = 0;
        int tmp = value;
        while (tmp > 0) { len++; tmp >>= 8; }
        byte[] result = new byte[len];
        for (int i = len - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private Object rlpDecode(byte[] data) {
        int[] pos = {0};
        return rlpDecodeItem(data, pos);
    }

    private Object rlpDecodeItem(byte[] data, int[] pos) {
        if (pos[0] >= data.length) return null;
        byte prefix = data[pos[0]];
        if ((prefix & 0xFF) < 0x80) {
            byte[] result = new byte[]{data[pos[0]]};
            pos[0]++;
            return result;
        } else if ((prefix & 0xFF) < 0xb8) {
            int len = (prefix & 0xFF) - 0x80;
            byte[] result = new byte[len];
            System.arraycopy(data, pos[0] + 1, result, 0, len);
            pos[0] += 1 + len;
            return result;
        } else if ((prefix & 0xFF) < 0xc0) {
            int lenOfLen = (prefix & 0xFF) - 0xb7;
            int len = parseLength(data, pos[0] + 1, lenOfLen);
            byte[] result = new byte[len];
            System.arraycopy(data, pos[0] + 1 + lenOfLen, result, 0, len);
            pos[0] += 1 + lenOfLen + len;
            return result;
        } else if ((prefix & 0xFF) < 0xf8) {
            int len = (prefix & 0xFF) - 0xc0;
            int startPos = pos[0] + 1;
            int endPos = startPos + len;
            java.util.List<byte[]> list = new java.util.ArrayList<>();
            while (pos[0] < endPos) {
                Object item = rlpDecodeItem(data, pos);
                if (item instanceof byte[]) {
                    list.add((byte[]) item);
                }
            }
            return list;
        } else {
            int lenOfLen = (prefix & 0xFF) - 0xf7;
            int len = parseLength(data, pos[0] + 1, lenOfLen);
            int startPos = pos[0] + 1 + lenOfLen;
            int endPos = startPos + len;
            java.util.List<byte[]> list = new java.util.ArrayList<>();
            pos[0] = startPos;
            while (pos[0] < endPos) {
                Object item = rlpDecodeItem(data, pos);
                if (item instanceof byte[]) {
                    list.add((byte[]) item);
                }
            }
            return list;
        }
    }

    private int parseLength(byte[] data, int pos, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result = (result << 8) | (data[pos + i] & 0xFF);
        }
        return result;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    // ===== RPC =====

    private String rpcCall(String method, JSONArray params) throws Exception {
        JSONObject req = new JSONObject();
        req.put("jsonrpc", "2.0");
        req.put("id", 1);
        req.put("method", method);
        req.put("params", params);

        RequestBody body = RequestBody.create(req.toString(),
                MediaType.parse("application/json"));
        Request request = new Request.Builder().url(ContractConfig.RPC_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            String respStr = response.body().string();
            JSONObject resp = new JSONObject(respStr);
            if (resp.has("error")) {
                throw new Exception("RPC error: " + resp.get("error"));
            }
            return resp.getString("result");
        }
    }
}
