# 合同审批管理系统

## 项目概述

基于 Spring Boot + Vue + Element UI 实现的合同审批管理系统，采用前后端分离的 Java BS 架构。支持合同从创建到归档的完整生命周期管理。

## 技术栈

### 后端
- **Spring Boot 2.7.18**: 核心框架
- **Spring Data JPA**: 数据持久层
- **H2 Database**: 嵌入式数据库，文件持久化模式
- **Hibernate Validator**: 参数校验
- **Lombok**: 简化代码
- **Apache Commons IO/Codec**: 文件处理和MD5加密

### 前端
- **Vue 2.7**: 前端框架
- **Element UI**: UI组件库
- **Axios**: HTTP客户端
- 纯静态页面，无需Node.js环境，通过CDN引入

## 项目结构

```
contract-approval-system/
├── src/
│   └── main/
│       ├── java/com/contract/
│       │   ├── ContractApplication.java          # 启动类
│       │   ├── common/
│       │   │   └── Result.java                   # 统一响应结果
│       │   ├── config/
│       │   │   ├── GlobalExceptionHandler.java   # 全局异常处理
│       │   │   └── WebConfig.java                # Web配置（静态资源、CORS）
│       │   ├── controller/
│       │   │   ├── ContractController.java       # 合同管理接口
│       │   │   └── AttachmentController.java     # 附件管理接口
│       │   ├── dto/
│       │   │   ├── ApprovalDTO.java              # 审批DTO
│       │   │   ├── ContractDTO.java              # 合同DTO
│       │   │   └── ContractQueryDTO.java         # 查询条件DTO
│       │   ├── entity/
│       │   │   ├── ApprovalRecord.java           # 审批记录实体
│       │   │   ├── Contract.java                 # 合同实体
│       │   │   └── ContractAttachment.java       # 合同附件实体
│       │   ├── enums/
│       │   │   ├── ApprovalAction.java           # 审批动作枚举
│       │   │   └── ContractStatus.java           # 合同状态枚举
│       │   ├── repository/
│       │   │   ├── ApprovalRecordRepository.java
│       │   │   ├── ContractAttachmentRepository.java
│       │   │   └── ContractRepository.java
│       │   └── service/
│       │       ├── AttachmentService.java
│       │       ├── ContractService.java
│       │       └── impl/
│       │           ├── AttachmentServiceImpl.java
│       │           └── ContractServiceImpl.java
│       └── resources/
│           ├── application.yml                   # 配置文件
│           └── static/                           # 前端静态资源
│               ├── index.html
│               ├── css/app.css
│               └── js/
│                   ├── api.js
│                   ├── app.js
│                   └── components/
│                       ├── approval-dialog.js
│                       ├── contract-detail.js
│                       └── contract-form.js
├── data/                                         # H2数据库文件目录（自动创建）
├── uploads/                                      # 附件上传目录（自动创建）
└── pom.xml
```

## 核心功能

### 合同生命周期状态流转

```
草稿(DRAFT) <--> 待审批(PENDING_APPROVAL) <--> 审批通过(APPROVED) --> 已归档(ARCHIVED)
                    ^       |
                    |       v
                    |   已驳回(REJECTED)
                    |       |
                    +-------+
                    |
                已撤回(WITHDRAWN)
```

### 状态说明

| 状态 | 说明 | 允许操作 |
|------|------|----------|
| 草稿 | 初始状态 | 编辑、上传附件、提交审批、删除 |
| 待审批 | 已提交等待审批 | 审批（通过/驳回）、撤回 |
| 审批通过 | 审批完成 | 归档 |
| 已驳回 | 审批不通过 | 编辑、重新提交 |
| 已撤回 | 撤回审批申请 | 编辑、重新提交 |
| 已归档 | 最终状态 | 仅查看 |

## 核心代码解析

### 1. 合同状态校验（ContractServiceImpl）

```java
private boolean isEditable(ContractStatus status) {
    return status == ContractStatus.DRAFT
            || status == ContractStatus.REJECTED
            || status == ContractStatus.WITHDRAWN;
}
```

所有状态变更操作都会在后端进行双重校验，即使前端按钮隐藏，后端API也会拒绝非法操作。

### 2. 动态查询Specification

使用JPA Specification实现动态条件查询，支持合同编号、客户名称、状态、负责人、金额范围、日期范围等多条件组合查询。

```java
Specification<Contract> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    // 动态添加查询条件
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

### 3. 审批记录自动记录

所有关键操作（创建、编辑、提交、审批、撤回、归档、上传/删除附件）都会自动记录审批日志，包含操作人、操作时间、操作意见。

```java
private void addApprovalRecord(Contract contract, ApprovalAction action, 
                               String operator, String remark) {
    ApprovalRecord record = new ApprovalRecord();
    record.setContract(contract);
    record.setAction(action);
    record.setOperator(operator);
    record.setRemark(remark);
    approvalRecordRepository.save(record);
}
```

### 4. 附件存储策略

- 附件文件存储在本地文件系统 `./uploads/` 目录下
- 数据库仅存储文件的相对路径、大小、MD5校验值
- 通过Spring Boot静态资源映射 `/uploads/**` 对外提供访问
- 文件名使用UUID重命名，避免冲突，按日期分目录存储

### 5. 全局异常处理

统一捕获并处理系统异常，返回友好的错误信息。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
```

### 6. 前端状态驱动的按钮控制

前端根据合同状态动态控制按钮的显示和禁用状态。

```javascript
function canEdit(status) {
    return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(status);
}
function canApprove(status) {
    return status === 'PENDING_APPROVAL';
}
```

## API接口列表

### 合同管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/contract/query | 分页查询合同列表 |
| GET | /api/contract/{id} | 查询合同详情 |
| POST | /api/contract | 新增合同 |
| PUT | /api/contract | 编辑合同 |
| DELETE | /api/contract/{id} | 删除合同（仅草稿） |
| POST | /api/contract/{id}/submit | 提交审批 |
| POST | /api/contract/{id}/approve | 审批通过 |
| POST | /api/contract/{id}/reject | 审批驳回 |
| POST | /api/contract/{id}/withdraw | 撤回审批 |
| POST | /api/contract/{id}/archive | 合同归档 |

### 附件管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/attachment/upload | 上传附件 |
| DELETE | /api/attachment/{id} | 删除附件 |
| GET | /api/attachment/contract/{id} | 查询合同附件列表 |
| GET | /api/attachment/download/{id} | 下载附件 |

## 数据库表结构

系统启动时JPA自动创建表结构：

1. **contract**: 合同主表
2. **contract_attachment**: 合同附件表
3. **approval_record**: 审批记录表

## 启动方式

### 环境要求
- JDK 1.8+
- Maven 3.6+

### 启动命令

```bash
# 方式1：直接运行
mvn spring-boot:run

# 方式2：打包后运行
mvn clean package
java -jar target/contract-approval-system-1.0.0.jar
```

### 访问地址

启动成功后访问：
- **系统首页**: http://localhost:8080/
- **H2数据库控制台**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/contract`
  - 用户名: `sa`
  - 密码: （空）

## 数据存储

- **数据库文件**: `./data/contract.mv.db`（H2文件模式，自动创建）
- **附件文件**: `./uploads/`（自动创建，按日期分目录存储）

## 使用说明

1. **新增合同**: 点击"新增合同"按钮，填写基本信息后保存
2. **上传附件**: 保存合同后，在编辑页面上传附件（提交审批前至少需要一个附件）
3. **提交审批**: 草稿状态下点击"提交"按钮
4. **审批处理**: 待审批状态下点击"审批"，选择通过或驳回
5. **撤回审批**: 待审批状态下点击"撤回"可撤回审批申请
6. **合同归档**: 审批通过后点击"归档"完成合同生命周期

## 安全说明

- 系统采用固定用户`admin`，未实现完整的用户认证授权（可根据需要扩展Spring Security）
- 所有状态变更操作后端都有校验，不依赖前端控制
- 附件上传路径做了隔离，无法通过路径遍历访问系统其他文件
