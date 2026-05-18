package com.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractDTO {
    private Long id;

    @NotBlank(message = "合同编号不能为空")
    private String contractNo;

    @NotBlank(message = "合同名称不能为空")
    private String contractName;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String partyA;
    private String partyB;

    @NotNull(message = "合同金额不能为空")
    private BigDecimal amount;

    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String responsiblePerson;
    private String department;
    private String contractType;
    private String content;
    private String creator;
}
