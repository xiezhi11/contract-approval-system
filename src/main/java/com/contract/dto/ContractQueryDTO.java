package com.contract.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractQueryDTO {
    private String contractNo;
    private String contractName;
    private String customerName;
    private String responsiblePerson;
    private String status;
    private String contractType;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
