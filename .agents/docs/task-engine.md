# AI 任务执行引擎说明文档

## 概述

本文档说明 AI 如何自动读取任务配置文件、编排执行顺序、生成代码和文档，实现自动化开发工作流。

## 工作原理

### 1. 任务发现与加载

```
AI启动
    ↓
扫描 .agents/tasks/*.yaml
    ↓
解析YAML文件，构建任务树
    ↓
验证任务格式和依赖关系
```

**AI操作**：
```python
# 伪代码示例
tasks = load_all_tasks_from(".agents/tasks/")
task_graph = build_dependency_graph(tasks)
validate_no_circular_dependencies(task_graph)
```

### 2. 任务状态机

```
┌─────────┐
│ pending │  初始状态
└────┬────┘
     │ (检查依赖)
     ↓
┌─────────┐
│  ready  │  依赖已满足，可执行
└────┬────┘
     │ (AI开始执行)
     ↓
┌──────────────┐
│ in_progress  │  执行中
└──────┬───────┘
       │
       ├─→ (成功) ┌───────────┐
       │          │ completed │  完成
       │          └───────────┘
       │
       └─→ (失败) ┌─────────┐
                  │ failed  │  失败（可重试）
                  └─────────┘
```

### 3. 执行流程

#### 步骤1：查找可执行任务

AI遍历所有任务，找出满足以下条件的任务：
- ✅ `status = "pending"` 或 `status = "ready"`
- ✅ 所有 `dependencies` 中的任务状态为 `completed`
- ✅ 所属 Milestone/Epic 未被阻塞

#### 步骤2：任务优先级排序

排序规则（优先级从高到低）：
1. **Milestone deadline**：截止时间近的优先
2. **Priority**：critical > high > medium > low
3. **Task type**：database_design > scaffolding > development > documentation
4. **Task ID**：字典序（保证确定性）

#### 步骤3：执行任务

根据 `task.execution.action` 执行不同操作：

| Action | 说明 | AI操作 |
|--------|------|--------|
| `create_file` | 创建文件 | 使用模板或生成内容，调用 `Create` 工具 |
| `generate_code` | 生成代码 | 根据需求生成Java/JavaScript代码 |
| `run_command` | 执行命令 | 调用 `Execute` 工具运行Shell命令 |
| `create_spring_boot_project` | 创建Spring Boot项目 | 生成pom.xml、目录结构、启动类 |
| `design_database` | 设计数据库 | 生成SQL建表语句和ER图 |
| `write_documentation` | 编写文档 | 生成Markdown文档 |

**示例：执行 create_file 任务**

```yaml
tasks:
  - id: "T0.1.1.1"
    title: "创建技术架构文档"
    type: "documentation"
    execution:
      action: "create_file"
      template: "tech_stack_template"
    deliverables:
      - path: ".agents/docs/tech-stack.md"
```

AI执行逻辑：
```python
# 1. 读取任务
task = get_task("T0.1.1.1")

# 2. 更新状态
update_task_status(task.id, "in_progress")

# 3. 生成内容（使用模板或AI生成）
content = generate_content_from_template("tech_stack_template", task.context)

# 4. 创建文件
create_file(".agents/docs/tech-stack.md", content)

# 5. 验证交付物
if file_exists(".agents/docs/tech-stack.md"):
    update_task_status(task.id, "completed")
else:
    update_task_status(task.id, "failed")
```

#### 步骤4：验证验收标准

执行完成后，AI检查 `acceptance_criteria`：

```yaml
acceptance_criteria:
  - "文件已创建"
  - "包含技术栈选型章节"
  - "包含架构图"
```

AI验证逻辑：
- 检查文件是否存在
- 检查文件内容是否包含关键词（如"技术栈"、"架构图"）
- 运行自动化测试（如代码编译、单元测试）

#### 步骤5：更新任务状态

- ✅ 所有验收标准通过 → `status = "completed"`
- ❌ 任何验收标准失败 → `status = "failed"`，记录失败原因

### 4. 批量处理

AI可以并行执行多个无依赖关系的任务：

```
Task A ─┐
Task B ─┼─→ 并行执行（无依赖）
Task C ─┘

Task D ──→ 等待 A、B、C 完成后执行（有依赖）
```

### 5. 任务更新机制

AI执行任务时，会更新YAML文件中的任务状态：

**执行前：**
```yaml
- id: "T0.1.1.1"
  status: "pending"
  started_at: null
  completed_at: null
```

**执行中：**
```yaml
- id: "T0.1.1.1"
  status: "in_progress"
  started_at: "2025-10-23T00:25:00Z"
  completed_at: null
```

**执行后：**
```yaml
- id: "T0.1.1.1"
  status: "completed"
  started_at: "2025-10-23T00:25:00Z"
  completed_at: "2025-10-23T00:27:30Z"
  actual_minutes: 2.5
  deliverables_created:
    - ".agents/docs/tech-stack.md"
```

## AI执行模式

### 模式1：交互式执行（推荐）

用户发送命令：`@ai execute-tasks`

AI 响应：
```
🔍 扫描任务文件...
📋 发现 15 个任务，3 个可执行

可执行任务：
1. [T0.1.1.1] 创建技术架构文档 (Priority: high)
2. [T0.2.1.1] 创建后端项目骨架 (Priority: high)
3. [T0.3.1.1] 设计数据库表结构 (Priority: high)

是否执行所有任务？(y/n)
```

用户确认后，AI开始执行并实时报告进度。

### 模式2：自动执行

在任务文件中启用自动模式：

```yaml
config:
  auto_execution: true
  execution_policy:
    mode: "aggressive"  # aggressive | conservative
    max_parallel_tasks: 3
    require_confirmation: false
```

AI会自动发现并执行任务，无需人工干预。

### 模式3：单任务执行

用户指定特定任务：`@ai execute-task T0.1.1.1`

AI仅执行该任务及其依赖。

## 错误处理

### 常见错误及处理策略

| 错误类型 | AI处理策略 |
|---------|-----------|
| 依赖循环 | 检测并报告循环路径，阻止执行 |
| 文件冲突 | 备份现有文件，记录冲突，请求人工决策 |
| 执行超时 | 标记为failed，记录日志，允许重试 |
| 验收失败 | 分析失败原因，尝试自动修复或请求人工介入 |
| 语法错误 | 运行Linter，自动修复或报告详细错误 |

### 重试机制

任务失败后，AI可以自动重试：

```yaml
tasks:
  - id: "T1.2.3.4"
    retry_policy:
      max_retries: 3
      retry_delay_seconds: 60
      backoff_multiplier: 2  # 指数退避
```

## 人工干预点

某些任务需要人工确认或输入：

```yaml
tasks:
  - id: "T_DEPLOY"
    title: "部署到生产环境"
    type: "deployment"
    assigned_to: "Human"  # 需要人工执行
    require_approval: true
    approval_from: "项目经理"
```

AI会暂停执行，等待人工确认后继续。

## 进度报告

AI定期生成进度报告：

```markdown
# SQL血缘分析项目进度报告
生成时间: 2025-10-23 00:30

## 里程碑进度
- [✅] M0: 需求与设计 (100%) - 已完成
- [🔄] M1: 核心引擎原型 (45%) - 进行中
- [⏸️] M2: 完整解析能力 (0%) - 未开始

## 今日完成任务
1. ✅ T0.1.1.1 - 创建技术架构文档
2. ✅ T0.2.1.1 - 创建后端项目骨架
3. ✅ T0.3.1.1 - 设计数据库表结构

## 进行中任务
1. 🔄 T1.1.2.3 - 实现字段依赖追踪算法 (60%)

## 阻塞问题
- ❌ T1.3.4.5 - Druid版本兼容性问题（等待解决方案）

## 风险提示
- ⚠️ M2截止时间临近，建议增加资源
```

## 使用示例

### 示例1：启动AI执行引擎

**用户输入：**
```
@ai start-task-engine
```

**AI输出：**
```
🚀 AI任务执行引擎已启动

📂 加载任务文件: .agents/tasks/sql-lineage.yaml
✅ 任务验证通过：5个里程碑，23个Epic，87个任务

🔍 分析依赖关系...
✅ 无循环依赖

📊 任务统计：
- Pending: 85
- Ready: 2
- Completed: 0

🎯 准备执行首批任务...
```

### 示例2：执行特定里程碑

**用户输入：**
```
@ai execute-milestone M0
```

**AI输出：**
```
🎯 执行里程碑 M0: 需求与设计

📋 任务清单：
1. [T0.1.1.1] 创建技术架构文档
2. [T0.1.2.1] 设计元数据表结构
3. [T0.2.1.1] 创建后端项目骨架

⏱️ 预计耗时: 2小时

开始执行...

✅ [1/3] T0.1.1.1 完成 (耗时: 1分30秒)
   📄 创建文件: .agents/docs/tech-stack.md

✅ [2/3] T0.1.2.1 完成 (耗时: 3分15秒)
   📄 创建文件: docs/schema.sql
   📄 创建文件: docs/er-diagram.md

✅ [3/3] T0.2.1.1 完成 (耗时: 2分45秒)
   📄 创建文件: backend/pom.xml
   📁 创建目录结构: backend/src/...

🎉 里程碑 M0 已完成！
```

## 配置文件位置

```
F:\00Code\droid3\
└── .agents/
    ├── tasks/
    │   └── sql-lineage.yaml      # 任务配置文件
    ├── docs/
    │   ├── task-format.md         # 本规范文档
    │   └── task-engine.md         # 本引擎文档
    └── logs/
        └── execution-2025-10-23.log  # 执行日志
```

## 下一步

AI现在可以：
1. ✅ 读取任务配置文件
2. ✅ 解析任务依赖关系
3. ✅ 按优先级执行任务
4. ✅ 生成代码和文档
5. ✅ 更新任务状态
6. ✅ 生成进度报告

**立即开始：创建第一个任务文件 `sql-lineage.yaml`！**
