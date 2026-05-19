package com.contract.controller;

import com.contract.common.Result;
import com.contract.dto.ContractDTO;
import com.contract.dto.ContractQueryDTO;
import com.contract.entity.Contract;
import com.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contract")
@CrossOrigin
public class ContractController {

    @Autowired
    private ContractService contractService;

    @PostMapping("/query")
    public Result<Page<Contract>> queryPage(@RequestBody ContractQueryDTO queryDTO) {
        return Result.success(contractService.queryPage(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Contract> getDetail(@PathVariable Long id) {
        return Result.success(contractService.getDetail(id));
    }

    @PostMapping
    public Result<Contract> create(@Validated @RequestBody ContractDTO dto) {
        return Result.success(contractService.create(dto));
    }

    @PutMapping
    public Result<Contract> update(@Validated @RequestBody ContractDTO dto) {
        return Result.success(contractService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    public Result<Contract> submitApproval(@PathVariable Long id) {
        return Result.success(contractService.submitApproval(id, null));
    }

    @PostMapping("/{id}/approve")
    public Result<Contract> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params) {
        String remark = params != null ? params.get("remark") : null;
        return Result.success(contractService.approve(id, null, remark));
    }

    @PostMapping("/{id}/reject")
    public Result<Contract> reject(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String remark = params.get("remark");
        return Result.success(contractService.reject(id, null, remark));
    }

    @PostMapping("/{id}/withdraw")
    public Result<Contract> withdraw(@PathVariable Long id) {
        return Result.success(contractService.withdraw(id, null));
    }

    @PostMapping("/{id}/archive")
    public Result<Contract> archive(@PathVariable Long id) {
        return Result.success(contractService.archive(id, null));
    }
}
