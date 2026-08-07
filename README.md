# MCKAI 模组工坊

> 融合 RikkaHub + Operit + ModCrafting 三大 App 核心功能的 AI 驱动 Minecraft 模组开发助手

<p align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" width="120" />
</p>

<p align="center">
  <b>Android 原生 | Kotlin + Jetpack Compose | Material 3</b><br>
  多模型 AI 对话 · 智能模组生成 · 完整项目管理 · 工具调用引擎
</p>

---

## 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术架构](#技术架构)
- [支持的 AI 模型](#支持的-ai-模型)
- [工具系统](#工具系统)
- [Agent 引擎](#agent-引擎)
- [模组工坊](#模组工坊)
- [项目管理](#项目管理)
- [界面设计](#界面设计)
- [安装与使用](#安装与使用)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [致谢](#致谢)
- [开源协议](#开源协议)

---

## 项目简介

MCKAI 模组工坊是一款基于 Android 原生开发的 AI 辅助 Minecraft 模组开发工具。它将三大知名开源项目的核心理念融合为一：

| 参考项目 | 核心特性 | MCKAI 集成情况 |
|---------|---------|--------------|
| **[RikkaHub](https://github.com/rikkahub/rikkahub)** | 多 Provider LLM 客户端、消息分支、Markdown 渲染、MCP 协议 | 多 Provider 支持、消息树结构、富文本渲染 |
| **[Operit](https://github.com/AAswordman/Operit)** | AI Agent 平台、40+ 工具、工作流引擎、JS 扩展桥 | 工具注册中心、25+ 内置工具、Agent 三模式路由 |
| **[ModCrafting](https://github.com/newstarbar/ModCrafting)** | Minecraft 模组 AI 生成、三模式路由、Vibecoding Agent | 模组生成引擎、Plan/Execute 双阶段、5 平台脚手架 |

### 设计理念

- **AI 驱动**：自然语言描述即可生成完整可编译的模组项目
- **多模型支持**：接入 OpenAI、Gemini、Claude、DeepSeek、通义千问等主流模型
- **工具调用**：LLM 可调用文件操作、网络请求、计算、Minecraft 数据查询等工具
- **平台全覆盖**：Fabric、Forge、NeoForge、Bedrock 基岩版、网易版
- **原生体验**：Kotlin + Compose，Material 3 设计语言，暗色/亮色主题

---

## 核心功能

### 1. 多模型 AI 对话

```
支持 6 种 Provider 类型：
├── OpenAI          (GPT-4.1, GPT-4o, o3, o4-mini)
├── Gemini          (Gemini 2.5 Pro/Flash)
├── Claude          (Claude Opus 4, Sonnet 4)
├── OpenRouter      (200+ 模型统一网关)
├── Ollama 本地     (Llama 3.1, Qwen 2.5, DeepSeek-R1)
└── 自定义兼容      (任何 OpenAI 兼容 API)
```

**功能亮点：**
- 流式输出（SSE Server-Sent Events）
- 工具调用（Function Calling）
- 推理过程展示（Reasoning/Thinking）
- Provider/模型热切换
- API Key 池轮转
- 自定义 HTTP 头
- 连接测试

### 2. 工具调用系统

LLM 在对话中可自动调用以下工具：

| 类别 | 工具 | 说明 |
|------|------|------|
| **核心** | `calculator` | 数学表达式求值（支持三角函数、幂运算） |
| | `get_current_time` | 获取当前日期时间 |
| | `generate_uuid` | 生成 UUID |
| | `random_number` | 生成随机数 |
| | `format_json` | JSON 格式化 |
| | `encode_base64` / `decode_base64` | Base64 编解码 |
| | `hash_text` | SHA-256/MD5 哈希 |
| | `url_encode` / `url_decode` | URL 编解码 |
| | `get_system_info` | 设备系统信息 |
| **文件** | `read_file` | 读取文件内容 |
| | `write_file` | 写入文件（需管理员权限） |
| | `list_directory` | 列出目录内容 |
| | `search_files` | 全文搜索文件 |
| | `delete_file` | 删除文件（需管理员权限） |
| | `create_directory` | 创建目录 |
| **网络** | `http_request` | 发送 HTTP 请求 |
| | `fetch_url` | 获取 URL 内容 |
| | `web_search` | DuckDuckGo 搜索 |
| **Minecraft** | `mc_version_info` | MC 版本与加载器信息 |
| | `mc_biome_data` | 生物群系数据查询 |
| | `mc_recipe_check` | 合成配方查询 |
| | `mc_mod_template` | 模组模板代码生成 |
| **记忆** | `save_memory` | 保存长期记忆 |
| | `search_memory` | 搜索记忆库 |

### 3. Agent 三模式引擎

```
用户输入 → 模式分类器 → 路由到对应处理器
                        ├── Chat 模式 → 普通对话（无工具调用）
                        ├── Plan 模式 → 结构化规划（最多 15 轮只读分析）
                        └── Execute 模式 → 逐步执行 + 工具调用（最多 40 轮）
```

- **Chat 模式**：日常问答、概念解释
- **Plan 模式**：分析需求、输出结构化计划（目标/步骤/文件/注意事项）
- **Execute 模式**：实际编码、调用工具、生成文件

### 4. 模组工坊

四步生成流程：

```
选择平台 → 描述模组 → AI 生成 → 查看结果
   │           │          │          │
   │           │          │          ├── 查看生成的文件
   │           │          │          ├── 导出为 ZIP 项目
   │           │          │          └── 保存到项目库
   │           │          │
   │           │          ├── 工具调用写文件
   │           │          ├── 文本解析 fallback
   │           │          └── 进度日志实时显示
   │           │
   │           ├── 名称/Mod ID/MC 版本
   │           ├── 功能描述
   │           └── 选择 AI 模型
   │
   ├── Java Fabric (1.21.4)
   ├── Java Forge (1.20.1)
   ├── Java NeoForge (1.21.1)
   ├── Bedrock 基岩版 (1.21)
   └── NetEase 网易版 (1.20.10)
```

**支持的模板类型：**
- 方块 (Block)
- 物品 (Item)
- 实体 (Entity)
- 食物 (Food)
- 工具 (Tool)
- 盔甲 (Armor)
- 合成配方 (Recipe)

---

## 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                     UI Layer                        │
│  ChatScreen │ WorkshopScreen │ ProjectsScreen │ ... │
├─────────────────────────────────────────────────────┤
│                  ViewModel Layer                    │
│  ChatVM │ WorkshopVM │ ProjectsVM │ SettingsVM     │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                      │
│  AgentEngine │ ToolRegistry │ WorkshopAgent         │
├─────────────────────────────────────────────────────┤
│                    Data Layer                       │
│  LlmClient │ AppDatabase │ SettingsRepository       │
├─────────────────────────────────────────────────────┤
│                  Foundation Layer                   │
│  OkHttp │ Room │ DataStore │ Kotlin Serialization   │
└─────────────────────────────────────────────────────┘
```

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0.21 | 主开发语言 |
| Jetpack Compose | BOM 2024.12.01 | 声明式 UI 框架 |
| Material 3 | BOM 管理 | UI 设计系统 |
| Room | 2.6.1 | 本地数据库（10 张表） |
| OkHttp | 4.12.0 | HTTP 客户端（SSE 流式） |
| Kotlin Serialization | 1.7.3 | JSON 序列化 |
| DataStore | 1.1.1 | 键值对存储 |
| Navigation Compose | 2.8.5 | 页面导航 |
| KSP | 2.0.21-1.0.28 | Room 注解处理 |

### 构建配置

```kotlin
android {
    namespace = "com.mckai.app"
    compileSdk = 35
    minSdk = 26      // Android 8.0+
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
}
```

---

## 支持的 AI 模型

### 预置 Provider

| Provider | 类型 | 推荐模型 | 特点 |
|----------|------|---------|------|
| OpenAI | OpenAI | GPT-4.1 | 工具调用最佳 |
| DeepSeek | 自定义兼容 | deepseek-chat | 中文能力强 |
| 通义千问 | 自定义兼容 | qwen-max | 国内模型 |
| Kimi | 自定义兼容 | moonshot-v1-auto | 长上下文 |
| 豆包 | 自定义兼容 | doubao-pro-256k | 字节跳动 |
| Ollama 本地 | Ollama | llama3.1 | 无需 API Key |
| Gemini | Gemini | gemini-2.5-flash | Google 模型 |
| Claude | Claude | claude-sonnet-4 | Anthropic |
| OpenRouter | OpenRouter | openai/gpt-4.1 | 200+ 模型网关 |

### 使用 OpenRouter

OpenRouter 是统一的模型网关，一个 API Key 即可访问 200+ 模型：

```
Provider: OpenRouter
Base URL: https://openrouter.ai/api/v1
API Key:  sk-or-v1-xxxxx
Model:    openai/gpt-4.1
          anthropic/claude-sonnet-4
          google/gemini-2.5-flash
          deepseek/deepseek-chat
```

### 使用 Ollama 本地模型

无需 API Key，直接连接本地 Ollama 服务：

```
Provider: Ollama 本地
Base URL: http://localhost:11434/v1
API Key:  ollama
Model:    llama3.1
          qwen2.5
          deepseek-r1
          codellama
```

---

## 工具系统

### 架构设计

```
ToolRegistry（注册中心）
├── ToolMetadata（工具元数据：名称/描述/参数Schema/权限/分类）
├── handler: suspend (JsonObject) -> String（执行函数）
├── ToolPermission（权限分级：STANDARD/ADMIN/ROOT）
└── 分类索引（core/file/network/minecraft/memory）
```

### 注册新工具

```kotlin
// 在 CoreTools.kt 或新文件中注册
r.register(ToolMetadata(
    name = "my_tool",
    description = "工具描述",
    parameters = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("properties", buildJsonObject {
            put("param1", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("参数说明"))
            })
        })
        put("required", buildJsonArray { add(JsonPrimitive("param1")) })
    },
    category = "custom"
) { args ->
    val param1 = args["param1"]?.jsonPrimitive?.content ?: return@register "缺少参数"
    "执行结果：$param1"
})
```

### LLM 工具调用流程

```
用户消息 → LLM 分析 → 决定调用工具 → 返回 tool_calls
                                          ↓
              ToolRegistry.execute(name, args) → 结果
                                          ↓
              结果作为 tool message 发回 LLM → 继续生成
```

---

## Agent 引擎

### 三模式路由

```kotlin
class AgentEngine(llmClient, toolRegistry) {
    fun run(config, userMessage, history) = flow {
        val mode = classifyMode(userMessage)  // 关键词分类
        when (mode) {
            CHAT   → chatMode(...)     // 普通对话
            PLAN   → planMode(...)     // 结构化规划
            EXECUTE → executeMode(...) // 工具调用执行
        }
    }
}
```

### 模式分类规则

| 模式 | 触发关键词 |
|------|-----------|
| Chat | 默认，无特殊关键词 |
| Plan | "分析"、"规划"、"计划"、"设计"、"架构"、"怎么做" |
| Execute | "生成"、"写"、"创建"、"实现"、"开发"、"编码"、"build"、"fix" |

### Execute 模式循环

```
循环（最多 40 轮）：
  1. 发送消息给 LLM
  2. 收集响应（文本 + 工具调用）
  3. 如果有工具调用：
     a. 执行工具
     b. 将结果作为 tool message 加入历史
     c. 继续下一轮
  4. 如果无工具调用或达到上限：
     a. 保存助手消息
     b. 结束
```

---

## 模组工坊

### 生成流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  选择平台    │ →  │  描述模组    │ →  │  AI 生成    │ →  │  查看结果    │
│             │    │             │    │             │    │             │
│  5 种平台    │    │  名称/版本   │    │  工具调用    │    │  文件列表    │
│  卡片选择    │    │  描述/功能   │    │  实时日志    │    │  导出 ZIP    │
│             │    │  模型选择    │    │  可取消      │    │  保存项目    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### 支持的平台

| 平台 | MC 版本 | 加载器 | 生成内容 |
|------|---------|--------|---------|
| Java Fabric | 1.21.4 | Fabric Loader 0.16.10 | build.gradle + fabric.mod.json + 入口类 |
| Java Forge | 1.20.1 | Forge 47.3.0 | build.gradle + mods.toml + 入口类 |
| Java NeoForge | 1.21.1 | NeoForge 21.1.x | build.gradle + mods.toml + 入口类 |
| Bedrock 基岩版 | 1.21 | Behavior Pack | manifest.json + 脚本 |
| NetEase 网易版 | 1.20.10 | 网易模组系统 | mod.json + Python 入口 |

### 生成的项目结构（以 Fabric 为例）

```
mymod/
├── build.gradle              # Fabric Loom 构建脚本
├── gradle.properties         # 版本配置
├── settings.gradle           # 项目设置
├── src/main/
│   ├── java/com/example/
│   │   └── MyMod.java        # 主类（ClientModInitializer）
│   └── resources/
│       ├── fabric.mod.json   # 模组元数据
│       └── assets/mymod/
│           └── icon.png      # 模组图标
```

---

## 项目管理

### 数据模型

```
AppDatabase (Room)
├── conversations        # 对话列表
│   ├── id, title, createdAt, updatedAt
│   ├── branchParentId   # 分支父对话
│   ├── assistantId      # 绑定的助手
│   └── isFavorite       # 是否收藏
├── messages             # 消息
│   ├── id, conversationId, role, content
│   ├── parentId         # 消息树父节点
│   ├── reasoningContent # 推理过程
│   ├── toolCallsJson    # 工具调用
│   └── isFavorite       # 收藏
├── message_attachments  # 附件
├── assistants           # 自定义助手
├── memories             # 长期记忆
├── workflows            # 工作流
├── tool_packages        # 工具包
├── favorites            # 收藏夹
├── projects             # 模组项目
└── project_files        # 项目文件
```

---

## 界面设计

### 底部导航

```
┌──────────────────────────────────┐
│                                  │
│          内容区域                 │
│                                  │
├──────────────────────────────────┤
│  💬 对话  │  🔨 工坊  │  📁 项目  │  ⚙️ 设置  │
└──────────────────────────────────┘
```

### 对话界面

```
┌──────────────────────────────────┐
│  ← 对话            GPT-4.1  🔧  │
├──────────────────────────────────┤
│                                  │
│     ┌─────────────────┐          │
│     │ 用户消息         │    👤   │
│     └─────────────────┘          │
│                                  │
│  ┌─────────────────┐             │
│  │ AI 回复内容      │    🤖     │
│  │ 支持 Markdown    │             │
│  │ 代码高亮         │             │
│  └─────────────────┘             │
│                                  │
├──────────────────────────────────┤
│  [输入消息...]          [发送 ➤] │
└──────────────────────────────────┘
```

### 工坊界面

```
┌──────────────────────────────────┐
│  ← 选择平台                       │
├──────────────────────────────────┤
│  ┌────────────────────────────┐  │
│  │ ☑ Java Fabric    1.21.4   │  │
│  │   Fabric 轻量级模组加载器    │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │   Java Forge     1.20.1   │  │
│  │   Forge 经典模组加载器      │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │   Java NeoForge  1.21.1   │  │
│  │   NeoForge 新一代          │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │   Bedrock 基岩版  1.21    │  │
│  │   Add-on/Behavior Pack    │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │   NetEase 网易版  1.20.10 │  │
│  │   网易中国版模组            │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

### 设置界面

```
┌──────────────────────────────────┐
│  设置                             │
├──────────────────────────────────┤
│  外观                             │
│  [跟随系统] [浅色] [深色]          │
│                                  │
│  AI 模型                     [+] │
│  ┌────────────────────────────┐  │
│  │ OpenAI / GPT-4.1        > │  │
│  │ DeepSeek / deepseek-chat > │  │
│  │ 通义千问 / qwen-max      > │  │
│  └────────────────────────────┘  │
│                                  │
│  功能                             │
│  记忆功能              [ON/OFF]  │
│                                  │
│  关于                             │
│  MCKAI 模组工坊 v1.0.0           │
│  融合 RikkaHub + Operit          │
│  + ModCrafting 的 AI 助手        │
└──────────────────────────────────┘
```

---

## 安装与使用

### 环境要求

- Android 8.0+（API 26）
- 有效的 AI 模型 API Key（或本地 Ollama）
- 网络连接（使用云端模型时）

### 安装步骤

1. **构建 APK**
   ```bash
   # 需要 Android SDK 和 Gradle
   gradle :app:assembleDebug
   ```

2. **安装到设备**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **首次配置**
   - 打开应用，进入「设置」
   - 添加 AI 模型（选择预设或自定义）
   - 填入 API Key
   - 测试连接

4. **开始使用**
   - **对话**：直接与 AI 聊天，讨论模组开发
   - **工坊**：选择平台 → 描述模组 → 一键生成
   - **项目**：管理已生成的模组项目

### 快速上手示例

**对话中生成模组：**
```
用户：帮我写一个 Minecraft 1.21.4 Fabric 模组，添加一个自定义方块"魔法石"，
     右键点击会发出粒子效果。

AI：[分析需求] → [调用 mc_mod_template 工具] → [生成 build.gradle] 
    → [生成 fabric.mod.json] → [生成 MagicStoneBlock.java] 
    → [生成客户端入口类]
```

**工坊中生成项目：**
1. 选择「Java Fabric」平台
2. 名称：Magic Stones
3. Mod ID：magicstones
4. 描述：添加魔法石方块，右键产生粒子效果
5. 点击「开始生成」
6. 等待 AI 生成完成
7. 导出 ZIP → 导入 IDE → 编译运行

---

## 项目结构

```
com.mckai.app/
├── MCKAIApp.kt                    # Application + DI 容器
├── MainActivity.kt                # 单 Activity 入口
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt         # Room 数据库（10 实体）
│   │   ├── entity/                 # 10 个实体类
│   │   └── dao/                   # 10 个 DAO 接口
│   ├── llm/
│   │   ├── LlmClient.kt           # 核心 LLM HTTP 客户端
│   │   ├── LlmModels.kt           # 共享数据模型
│   │   ├── ProviderConfig.kt      # Provider 配置 + 9 预设
│   │   ├── SseParser.kt           # SSE/NDJSON 流解析器
│   │   └── StreamAccumulator.kt   # 流聚合器
│   └── repo/
│       ├── SettingsRepository.kt  # DataStore 设置
│       └── ProjectRepository.kt   # Room 数据访问
│
├── domain/
│   ├── agent/
│   │   └── AgentEngine.kt         # 三模式 Agent 引擎
│   ├── tools/
│   │   ├── ToolRegistry.kt        # 工具注册中心
│   │   ├── CoreTools.kt           # 10 个核心工具
│   │   ├── FileTools.kt           # 6 个文件工具
│   │   ├── NetworkTools.kt        # 3 个网络工具
│   │   ├── MinecraftTools.kt      # 4 个 MC 工具
│   │   └── MemoryTools.kt         # 2 个记忆工具
│   └── workshop/
│       ├── AgentPrompts.kt        # 提示词工程
│       ├── WorkshopAgent.kt       # 模组生成 Agent
│       ├── ModEdition.kt          # 5 平台枚举 + 数据模型
│       └── ModExporter.kt         # ZIP 导出 + 脚手架
│
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt          # 路由定义
│   │   └── AppNavGraph.kt         # 导航图
│   ├── theme/
│   │   └── Theme.kt               # Material 3 主题
│   ├── components/
│   │   └── MarkdownText.kt        # Markdown 渲染器
│   ├── chat/
│   │   ├── ChatViewModel.kt       # 聊天 VM（工具循环）
│   │   ├── ChatScreen.kt          # 聊天界面
│   │   ├── ChatListViewModel.kt   # 列表 VM
│   │   └── ChatListScreen.kt      # 对话列表
│   ├── workshop/
│   │   ├── WorkshopViewModel.kt   # 工坊 VM
│   │   └── WorkshopScreen.kt      # 工坊界面（4 步）
│   ├── projects/
│   │   ├── ProjectsViewModel.kt   # 项目列表 VM
│   │   ├── ProjectsScreen.kt      # 项目列表
│   │   ├── ProjectDetailViewModel.kt
│   │   ├── ProjectDetailScreen.kt # 项目详情
│   │   ├── FileEditorViewModel.kt # 文件编辑 VM
│   │   └── FileEditorScreen.kt    # 文件编辑器
│   └── settings/
│       ├── SettingsViewModel.kt   # 设置 VM
│       ├── SettingsScreen.kt      # 设置界面
│       └── ProviderEditScreen.kt  # Provider 编辑
│
└── (util/)                        # 工具类
```

**总计：58 个 Kotlin 源文件，约 8,000+ 行代码**

---

## 开发指南

### 构建项目

```bash
# 前提条件
# - JDK 17
# - Android SDK (platforms;android-35, build-tools;35.0.0)
# - Gradle 8.11.1（已包含在项目中）

# 构建 Debug APK
gradle :app:assembleDebug

# 输出路径
app/build/outputs/apk/debug/app-debug.apk
```

### 添加新的 Provider

1. 在 `ProviderType.kt` 中添加枚举值（如需要）
2. 在 `LlmClient.kt` 的 `buildRequest()` 中添加请求构建逻辑
3. 在 `LlmClient.kt` 的 `parsePayload()` 中添加响应解析逻辑
4. 在 `ProviderPresets.builtIn()` 中添加预设配置

### 添加新的工具

1. 在 `domain/tools/` 下创建或编辑工具文件
2. 使用 `r.register(ToolMetadata(...), handler = { ... })` 注册
3. 工具会自动被 `ToolRegistry.buildDefault()` 加载
4. LLM 会通过 `getToolsForLlm()` 获取工具定义

### 数据库迁移

当前使用 `fallbackToDestructiveMigration()`，生产环境应编写 Migration：

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN assistantId INTEGER")
    }
}
```

---

## 致谢

本项目融合了以下优秀开源项目的核心理念：

| 项目 | 作者 | Stars | 贡献 |
|------|------|-------|------|
| [RikkaHub](https://github.com/rikkahub/rikkahub) | rererere | 6.7K | 多 Provider 架构、消息分支、Markdown 渲染 |
| [Operit](https://github.com/AAswordman/Operit) | AAswordman | 6.6K | Agent 工具系统、工作流引擎、JS 扩展桥 |
| [ModCrafting](https://github.com/newstarbar/ModCrafting) | newstarbar | 15 | 三模式路由、Vibecoding Agent、防御性护栏 |

---

## 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

---

<p align="center">
  <b>MCKAI 模组工坊</b> — 让 AI 帮你开发 Minecraft 模组
</p>
