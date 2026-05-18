package com.contract.service;

import com.contract.dto.ContractDTO;
import com.contract.dto.ContractQueryDTO;
import com.contract.entity.Contract;
import org.springframework.data.domain.Page;

public interface ContractService {
    Page<Contract> queryPage(ContractQueryDTO queryDTO);
    Contract getDetail(Long id);
    Contract create(ContractDTO dto);
    Contract update(ContractDTO dto);
    void delete(Long id);
    Contract submitApproval(Long id, String operator);
    Contract approve(Long id, String operator, String remark);
    Contract reject(Long id, String operator, String remark);
    Contract withdraw(Long id, String operator);
    Contract archive(Long id, String operator);
}
