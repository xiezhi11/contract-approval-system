package com.contract.enums;

public enum ApprovalAction {
    CREATE("创建"),
    UPDATE("编辑"),
    SUBMIT("提交审批"),
    APPROVE("审批通过"),
    REJECT("审批驳回"),
    WITHDRAW("撤回审批"),
    ARCHIVE("归档"),
    UPLOAD_ATTACHMENT("上传附件"),
    DELETE_ATTACHMENT("删除附件");

    private final String description;

    ApprovalAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
