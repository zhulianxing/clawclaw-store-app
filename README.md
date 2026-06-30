# CONVNET Store — 去中心化软件商店 APP

## 项目概述

一个 Android 软件商店应用，直接连接 Polygon 区块链智能合约，浏览、下载和购买端侧AI应用。

**特点**：
- 📱 原生 Android 应用
- 🔗 直接读取 Polygon 链上数据（无后端API依赖）
- 💳 纯链上支付（USDC），不接入法币支付
- 🎫 NFT 激活码系统（ERC-721）
- 🌐 暗色主题，与 daix.fun 网页版风格一致

## 架构

```
CONVNET Store APP (Android)
    │
    ├─ Web3Client ──→ Polygon RPC (只读查询)
    │                   ├─ AppRegistry: 读取APP列表
    │                   ├─ DeveloperRegistry: 开发者信息
    │                   ├─ LicenseNFT: 用户许可证
    │                   └─ USDC: 余额查询
    │
    ├─ 钱包导入 ──→ 本地私钥存储
    │                   └─ 签名交易: approve + mintLicense
    │
    └─ APK 下载 ──→ clawclaw.tech/apk/
                      └─ 自动安装
```

## 文件结构

```
convnet-store/
├── build.gradle                    # 项目级构建
├── settings.gradle
├── gradle.properties
├── app/
│   ├── build.gradle                # APP模块构建
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/convnet/store/
│       │   ├── StoreApp.java           # Application
│       │   ├── ContractConfig.java     # 合约地址 + APP元数据
│       │   ├── Web3Client.java         # Polygon RPC 客户端
│       │   ├── WalletManager.java      # 钱包管理
│       │   ├── AppInfo.java            # APP数据模型
│       │   ├── AppAdapter.java         # 列表适配器
│       │   ├── MainActivity.java       # 首页(应用列表)
│       │   ├── AppDetailActivity.java  # APP详情页
│       │   ├── WalletActivity.java     # 钱包导入页
│       │   └── MyLicensesActivity.java # 我的凭证页
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml       # 首页布局
│           │   ├── activity_detail.xml     # 详情页布局
│           │   ├── activity_wallet.xml     # 钱包页布局
│           │   ├── activity_licenses.xml   # 凭证页布局
│           │   └── item_app_card.xml       # APP卡片
│           ├── drawable/                # 背景样式
│           ├── values/
│           │   ├── strings.xml          # 字符串+颜色
│           │   └── styles.xml           # 主题
│           └── xml/
│               ├── file_paths.xml       # FileProvider
│               └── network_security_config.xml
```

## 链上合约 (Polygon 主网)

| 合约 | 地址 |
|------|------|
| DeveloperRegistry | `0x8EE775e943C31EA7c4eF2B7bb83e0651c9d04001` |
| AppRegistry | `0xdFCFA7d871cCc3674d873d00192Ba26a685B2b68` |
| LicenseNFT | `0x7Ed65226C66b188f66AA0e5483917B1C33a41225` |
| RevenueSplit | `0x698c00CD9e94353C6ef804e670bc558108250f9D` |
| USDC | `0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359` |

## 收益分配

| 角色 | 比例 |
|------|:---:|
| 创作者 | 60% |
| 推广者 | 25% |
| 平台 | 10% |
| Gas补贴 | 5% |

## 功能页面

### 1. 首页 (MainActivity)
- 顶部钱包栏（地址 + USDC/POL余额）
- 统计栏（APP数/创作者数/已售凭证数）
- APP列表（从链上实时读取）
- 下拉刷新

### 2. APP详情页 (AppDetailActivity)
- APP图标、名称、包名、描述
- 价格、试用天数、版本号
- 收益分配说明
- 下载试用按钮
- 购买数字凭证按钮（链上USDC支付）

### 3. 钱包导入页 (WalletActivity)
- 私钥导入
- 钱包地址 + 余额显示
- 安全提示

### 4. 我的凭证页 (MyLicensesActivity)
- 显示用户持有的 NFT 激活码列表
- Token ID + 对应APP信息

## 待完成 (TODO)

- [ ] 集成 Web3j 或 bouncycastle 实现私钥→地址推导
- [ ] 实现链上交易签名（USDC approve + mintLicense）
- [ ] 添加 WalletConnect 支持（可选）
- [ ] APP截图展示
- [ ] 搜索功能
- [ ] 分类筛选
- [ ] 下载进度通知
- [ ] 已购APP自动激活验证

## 构建

```bash
cd /Users/mac/Documents/Codex/convnet-store
./gradlew assembleRelease
# 输出: app/build/outputs/apk/release/app-release.apk
```

## 技术栈

- Android SDK 34 (minSdk 26, Android 8.0+)
- Java 17
- OkHttp 4.12 (RPC请求)
- Material Components (UI)
- 无Web3库依赖，直接JSON-RPC调用
