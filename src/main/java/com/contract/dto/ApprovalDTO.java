package com.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ApprovalDTO {
    private Long contractId;
    private String operator;

    @NotBlank(message = "审批意见不能为空", groups = {RejectGroup.class})
    private String remark;

    public interface RejectGroup {
    }
}
