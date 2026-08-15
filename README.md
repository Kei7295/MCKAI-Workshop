# MCKAI 模组工坊

Android 原生模组开发助手。聊天式 AI 交互：生成 Minecraft 模组（Fabric / Forge / NeoForge / Bedrock）骨架、管理项目文件、零导出一键分享 ZIP。

## 技术栈

| 层 | 选型 |
| --- | --- |
| UI | Jetpack Compose（Material 3 + Apple 风格组件库） |
| 架构 | MVVM + 单向数据流（UiState + sealed event） |
| 数据 | Room 4（schema v4）+ DataStore Preferences |
| 网络 | OkHttp 流式 SSE（增量 JSON 修复） |
| AI | 多 Provider 兼容：OpenAI / Gemini / Claude（OpenAI 兼容端点） |
| 字体 | DM Sans（界面）+ JetBrains Mono（代码） |

## 构建

```bash
gradle :app:assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk（约 17MB）
```

质量门：`gradle :app:lintDebug`（当前 0 error / 17 warning，全部为依赖版本升级提示）。

## 目录结构

```
app/src/main/java/com/mckai/app/
├── MCKAIApp.kt            # Application 入口，注入 db/repo/toolRegistry
├── MainActivity.kt        # 底部导航：聊天 / 模组工坊 / 项目 / 设置
├── data/
│   ├── db/                # Room：Message/Favorite/Project/File 实体 + v4 migration
│   ├── llm/               # LlmClient 流式客户端、SSE 解析、增量 JSON 修复
│   └── repo/              # SettingsRepository（DataStore 原子写）
├── domain/
│   ├── agent/             # AgentEngine：LLM 流 + 真工具循环（DAG 收敛）
│   ├── tools/             # memory / network / system / file 工具 + 权限门
│   └── workshop/          # WorkshopAgent（生成骨架）+ ModExporter（ZIP 导出）
└── ui/
    ├── chat/              # ChatScreen + ChatViewModel（生成状态机）
    ├── components/        # AppleUi.kt：按钮/卡片/导航栏/头像 等组件库
    ├── navigation/        # AppNavHost 路由表
    ├── projects/          # 项目列表 / 详情 / 文件编辑器
    ├── settings/          # Provider / 助手配置页
    ├── theme/             # AppleFonts（DM Sans + JetBrains Mono）、色板
    └── workshop/          # 工作台 + WorkshopViewModel
```

## 核心机制

### 流式 LLM（data/llm/LlmClient.kt）

- 统一契约 `LlmEvent`：`TextDelta` / `ReasoningDelta` / `ToolCallDelta` / `Error` / `Done(usage)`
- **流结束兜底**：无论正常结束、解析失败还是 HTTP 错误，`stream()` 保证恰好 emit 一次 `Done`（幂等标志），调用方不再依赖 provider 的终止事件
- **SSE 动态判定**：`SseParser` 首行不判死，确认 `data:` 前缀前按纯文本透传；仅当实测出现事件流才切换严格模式
- **Gemini 工具往返**：functionCall / functionResponse / inlineData（真实图片字节）完整支持；Claude 走 `message_delta`/`message_stop`；OpenAI 解析 `usage` 字段
- **增量 JSON 修复**：`fixJson()` 对截断的工具调用参数补闭合引号/括号，配合流式累积器产出可用参数

### 工具调用与权限门（domain/tools/ToolRegistry.kt）

- 工具分三级权限：`STANDARD`（对话可直接用）→ `ADMIN`（需用户批准）→ `ROOT`（永拒）
- `execute(name, args, allowSensitive=false)`：未显式放行时，ADMIN 工具一律拒绝，杜绝 AI 越权（删文件、改系统设置）

### 聊天生成状态机（ui/chat/ChatViewModel.kt)

- `send()` 同步置 `isGenerating` 再启动协程，杜绝双击竞态；返回 Boolean 决定输入框清空
- `stop()` 真实取消：`generationJob?.cancel()` + 批准门补完 + 状态复位
- `loadConversation()` 切换会话时取消旧观察 Job，防止旧消息回流污染新会话
- 错误收敛：任何异常路径 → `Error` 事件 → 状态复位，UI 只认 `error` 字段

### 模组工坊（domain/workshop/）

- `WorkshopAgent`：多轮工具循环（write_file / complete），全程透出 `Files` 事件（生成文件清单），取消时返回明确错误
- `ModExporter.exportZip`：Fabric/NeoForge 骨架 + `mods.toml`/`fabric.mod.json` 严格 JSON 转义、路径穿越清洗（拒绝 `../`）、1x1 透明占位 icon；项目详情页右上角分享按钮直接导出系统分享

### 数据层（data/db/）

- `MessageEntity` 自引用外键（parentId → CASCADE）；`FavoriteEntity` → messageId 外键；`ProjectFileEntity` 唯一索引 `(projectId, filePath)`
- schema v4 迁移 `MIGRATION_3_4`：重建三表并保留存量数据（无 destructive fallback）
- `SettingsRepository`：provider 增删改单次 `edit{}` 原子提交

## 已知取舍（刻意保留）

- **字体子集**：仅内嵌 latin 子集（DM Sans / JetBrains Mono），中文走系统字体回退——安装包 17MB 的代价
- **无 hover 态**：触屏优先，桌面 hover 反馈未实现
- **模型上下文**：memory 检索结果并入 systemPrompt，不占对话轮次
- **Gemini 无 system 角色**：OpenAI 无该限制；Gemini 分支将系统提示并入首条 user 消息

## 修复记录（2026-08 重大排查）

- LlmClient：Gemini/Claude 从不发 `Done` → 统一兜底；阻塞读未取消 → `invokeOnCompletion { call.cancel() }`；多模态图片被丢弃 → 真实 inlineData
- AgentEngine：工具只执行不回流 → 真循环（执行结果回喂模型，多轮直到无待执行调用）；限制 `maxRounds=40` 防死循环
- MemoryTools：假实现 → MemoryDao 读写；NetworkTools：主线程阻塞 + Response 泄漏 → IO 调度 + `use{}`
- ChatViewModel：停止不生效（改标志位而非取消协程）→ Job 真实取消
- Room：无外键约束/无唯一索引 → v4 补全；重复收藏 → IGNORE 冲突策略
- 路由：`chat/new` 与 `chat/{convId}` pattern 冲突 → 独立字面量 `new-chat` / `provider-new`