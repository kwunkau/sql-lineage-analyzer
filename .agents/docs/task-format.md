# 任务配置文件格式规范

## 概述

本文档定义了AI任务管理系统的任务配置文件格式，用于实现"用户发布任务 → AI自动处理 → 编排执行 → 交付成果"的工作流。

## 任务层级结构

```
Milestone (里程碑)
  └── Epic (史诗/大功能)
      └── Story (用户故事)
          └── Task (具体任务)
```

## YAML 配置格式

### 完整示例

```yaml
# 项目信息
project:
  name: "SQL Field Lineage Analyzer"
  description: "SQL字段级血缘分析平台"
  start_date: "2025-10-23"
  deadline: "2025-11-07"  # 15天
  owner: "数据工程团队"

# 里程碑列表
milestones:
  - id: "M0"
    name: "需求与设计"
    description: "明确需求和技术路线"
    priority: "critical"
    status: "in_progress"
    deadline: "Day 1"
    acceptance_criteria:
      - "需求文档完成"
      - "架构设计确定"
      - "数据库设计完成"
    deliverables:
      - ".agents/docs/requirements.md"
      - ".agents/docs/architecture.md"
      - "docs/database-design.sql"
    
    # Epic（功能模块）
    epics:
      - id: "E0.1"
        name: "技术方案设计"
        description: "完成技术选型和架构设计"
        priority: "high"
        status: "pending"
        estimated_hours: 4
        
        # Story（用户故事）
        stories:
          - id: "S0.1.1"
            title: "作为架构师，我需要设计前后端技术栈"
            priority: "high"
            status: "pending"
            acceptance_criteria:
              - "明确后端框架（Spring Boot + Druid）"
              - "明确前端框架（HTML/CSS/JS + G6）"
              - "明确数据库选型（H2/MySQL）"
            
            # Task（具体任务）
            tasks:
              - id: "T0.1.1.1"
                title: "创建技术架构文档"
                type: "documentation"
                priority: "high"
                status: "pending"
                estimated_minutes: 30
                dependencies: []
                assigned_to: "AI"
                deliverables:
                  - path: ".agents/docs/tech-stack.md"
                    description: "技术栈选型文档"
                execution:
                  action: "create_file"
                  template: "tech_stack_template"
                  
          - id: "S0.1.2"
            title: "作为开发者，我需要设计数据库结构"
            priority: "high"
            status: "pending"
            tasks:
              - id: "T0.1.2.1"
                title: "设计元数据表结构"
                type: "database_design"
                priority: "high"
                status: "pending"
                estimated_minutes: 60
                dependencies: ["T0.1.1.1"]
                assigned_to: "AI"
                deliverables:
                  - path: "docs/schema.sql"
                    description: "数据库建表SQL"
                  - path: "docs/er-diagram.md"
                    description: "ER图文档"

      - id: "E0.2"
        name: "项目初始化"
        description: "搭建项目骨架和目录结构"
        priority: "high"
        status: "pending"
        stories:
          - id: "S0.2.1"
            title: "作为开发者，我需要创建项目目录结构"
            tasks:
              - id: "T0.2.1.1"
                title: "创建后端项目骨架"
                type: "scaffolding"
                priority: "high"
                status: "pending"
                dependencies: ["T0.1.1.1"]
                assigned_to: "AI"
                deliverables:
                  - path: "backend/pom.xml"
                    description: "Maven配置文件"
                  - path: "backend/src/main/java/com/lineage/Application.java"
                    description: "Spring Boot启动类"
                execution:
                  action: "create_spring_boot_project"
                  parameters:
                    groupId: "com.lineage"
                    artifactId: "sql-lineage-analyzer"
                    dependencies:
                      - "spring-boot-starter-web"
                      - "druid:1.2.20"
                      - "mybatis-plus-boot-starter"

  - id: "M1"
    name: "核心引擎原型"
    description: "实现基础SQL解析和字段血缘分析"
    priority: "critical"
    status: "pending"
    deadline: "Day 3"
    dependencies: ["M0"]
    acceptance_criteria:
      - "可解析简单SELECT语句"
      - "可提取字段依赖关系"
      - "输出JSON格式血缘结果"
    epics:
      - id: "E1.1"
        name: "Druid SQL解析器集成"
        # ... 详细任务

  - id: "M2"
    name: "完整解析能力"
    description: "支持所有SQL类型和多数据库方言"
    priority: "high"
    status: "pending"
    deadline: "Day 7"
    dependencies: ["M1"]
    # ...

# 全局配置
config:
  task_status_enum:
    - "pending"      # 待处理
    - "ready"        # 依赖已满足，可开始
    - "in_progress"  # 进行中
    - "blocked"      # 阻塞（依赖未完成）
    - "completed"    # 已完成
    - "skipped"      # 跳过
    - "failed"       # 失败
  
  task_type_enum:
    - "development"      # 开发任务
    - "testing"          # 测试任务
    - "documentation"    # 文档任务
    - "database_design"  # 数据库设计
    - "scaffolding"      # 脚手架搭建
    - "deployment"       # 部署任务
    - "review"           # 代码审查
  
  priority_enum:
    - "critical"  # P0 - 必须完成
    - "high"      # P1 - 高优先级
    - "medium"    # P2 - 中优先级
    - "low"       # P3 - 低优先级
```

## 字段说明

### Milestone（里程碑）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | string | ✅ | 唯一标识，如 M0, M1 |
| name | string | ✅ | 里程碑名称 |
| description | string | ✅ | 详细描述 |
| priority | enum | ✅ | 优先级：critical/high/medium/low |
| status | enum | ✅ | 状态：pending/in_progress/completed |
| deadline | string | ✅ | 截止时间，如 "Day 1" 或日期 |
| dependencies | array | ❌ | 依赖的里程碑ID列表 |
| acceptance_criteria | array | ✅ | 验收标准（字符串列表） |
| deliverables | array | ✅ | 交付物路径列表 |
| epics | array | ✅ | 包含的Epic列表 |

### Epic（史诗）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | string | ✅ | 唯一标识，如 E0.1 |
| name | string | ✅ | Epic名称 |
| description | string | ✅ | 详细描述 |
| priority | enum | ✅ | 优先级 |
| status | enum | ✅ | 状态 |
| estimated_hours | number | ❌ | 预计工时 |
| stories | array | ✅ | 包含的Story列表 |

### Story（用户故事）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | string | ✅ | 唯一标识，如 S0.1.1 |
| title | string | ✅ | 用户故事标题（"作为...我需要..."格式） |
| priority | enum | ✅ | 优先级 |
| status | enum | ✅ | 状态 |
| acceptance_criteria | array | ✅ | 验收标准 |
| tasks | array | ✅ | 包含的Task列表 |

### Task（具体任务）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | string | ✅ | 唯一标识，如 T0.1.1.1 |
| title | string | ✅ | 任务标题 |
| type | enum | ✅ | 任务类型（见config.task_type_enum） |
| priority | enum | ✅ | 优先级 |
| status | enum | ✅ | 状态 |
| estimated_minutes | number | ❌ | 预计耗时（分钟） |
| dependencies | array | ❌ | 依赖的任务ID列表 |
| assigned_to | string | ✅ | 执行者：AI 或 Human |
| deliverables | array | ✅ | 交付物列表（见Deliverable结构） |
| execution | object | ❌ | AI执行指令（见Execution结构） |

### Deliverable（交付物）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| path | string | ✅ | 文件路径 |
| description | string | ✅ | 文件描述 |
| validation | object | ❌ | 验证规则（如文件必须存在、包含特定内容等） |

### Execution（执行指令）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| action | string | ✅ | 执行动作：create_file/generate_code/run_command |
| template | string | ❌ | 使用的模板名称 |
| parameters | object | ❌ | 执行参数 |

## AI执行规则

1. **任务扫描**：AI定期扫描 `.agents/tasks/*.yaml` 文件
2. **依赖检查**：找出所有 `status=pending` 且依赖已完成的任务
3. **优先级排序**：按 priority 和 Milestone 顺序排序
4. **任务执行**：
   - 更新状态为 `in_progress`
   - 根据 `execution.action` 执行操作
   - 生成 `deliverables` 中指定的文件
   - 验证 `acceptance_criteria`
5. **状态更新**：
   - 成功：更新为 `completed`
   - 失败：更新为 `failed`，记录错误信息
6. **递归执行**：继续处理下一批ready的任务

## 示例工作流

```
用户创建 sql-lineage.yaml
    ↓
AI读取配置文件
    ↓
解析M0里程碑 → E0.1 Epic → S0.1.1 Story → T0.1.1.1 Task
    ↓
检查依赖：无依赖，状态=ready
    ↓
执行任务：创建 .agents/docs/tech-stack.md
    ↓
验证交付物：文件已创建 ✓
    ↓
更新状态：T0.1.1.1 = completed
    ↓
触发下一个任务：T0.1.2.1（依赖T0.1.1.1）
```

## 最佳实践

1. **原子化任务**：每个Task应该是可独立完成的最小单元
2. **明确验收**：每个任务必须有清晰的 `acceptance_criteria`
3. **合理依赖**：避免循环依赖，保持DAG结构
4. **渐进式开发**：优先完成critical任务，逐步迭代
5. **文档驱动**：先完成设计文档，再开始编码

## 文件命名规范

```
.agents/tasks/
├── sql-lineage.yaml           # 主项目任务清单
├── milestone-m0.yaml          # M0里程碑详细任务（可选，拆分大文件）
├── milestone-m1.yaml          # M1里程碑详细任务
└── ...
```
