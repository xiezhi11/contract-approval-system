package com.contract.enums;

public enum ContractStatus {
    DRAFT("草稿"),
    PENDING_APPROVAL("待审批"),
    APPROVED("审批通过"),
    REJECTED("已驳回"),
    WITHDRAWN("已撤回"),
    ARCHIVED("已归档");

    private final String description;

    ContractStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
