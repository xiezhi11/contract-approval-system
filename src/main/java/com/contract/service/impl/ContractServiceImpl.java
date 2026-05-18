package com.contract.service.impl;

import com.contract.dto.ContractDTO;
import com.contract.dto.ContractQueryDTO;
import com.contract.entity.ApprovalRecord;
import com.contract.entity.Contract;
import com.contract.entity.ContractAttachment;
import com.contract.enums.ApprovalAction;
import com.contract.enums.ContractStatus;
import com.contract.repository.ApprovalRecordRepository;
import com.contract.repository.ContractRepository;
import com.contract.service.ContractService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ApprovalRecordRepository approvalRecordRepository;

    @Override
    public Page<Contract> queryPage(ContractQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageNum() - 1,
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Specification<Contract> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(queryDTO.getContractNo())) {
                predicates.add(cb.like(root.get("contractNo"), "%" + queryDTO.getContractNo() + "%"));
            }
            if (StringUtils.hasText(queryDTO.getContractName())) {
                predicates.add(cb.like(root.get("contractName"), "%" + queryDTO.getContractName() + "%"));
            }
            if (StringUtils.hasText(queryDTO.getCustomerName())) {
                predicates.add(cb.like(root.get("customerName"), "%" + queryDTO.getCustomerName() + "%"));
            }
            if (StringUtils.hasText(queryDTO.getResponsiblePerson())) {
                predicates.add(cb.like(root.get("responsiblePerson"), "%" + queryDTO.getResponsiblePerson() + "%"));
            }
            if (StringUtils.hasText(queryDTO.getStatus())) {
                predicates.add(cb.equal(root.get("status"), ContractStatus.valueOf(queryDTO.getStatus())));
            }
            if (StringUtils.hasText(queryDTO.getContractType())) {
                predicates.add(cb.equal(root.get("contractType"), queryDTO.getContractType()));
            }
            if (queryDTO.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), queryDTO.getMinAmount()));
            }
            if (queryDTO.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), queryDTO.getMaxAmount()));
            }
            if (queryDTO.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), queryDTO.getStartDate().atStartOfDay()));
            }
            if (queryDTO.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), queryDTO.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return contractRepository.findAll(spec, pageable);
    }

    @Override
    public Contract getDetail(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("合同不存在"));
    }

    @Override
    @Transactional
    public Contract create(ContractDTO dto) {
        if (contractRepository.existsByContractNo(dto.getContractNo())) {
            throw new RuntimeException("合同编号已存在");
        }

        Contract contract = new Contract();
        BeanUtils.copyProperties(dto, contract);
        contract.setStatus(ContractStatus.DRAFT);
        if (!StringUtils.hasText(contract.getCreator())) {
            contract.setCreator("admin");
        }
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.CREATE, contract.getCreator(), "创建合同");

        return contract;
    }

    @Override
    @Transactional
    public Contract update(ContractDTO dto) {
        Contract contract = getDetail(dto.getId());

        if (!isEditable(contract.getStatus())) {
            throw new RuntimeException("当前状态不允许编辑");
        }

        if (contractRepository.existsByContractNoAndIdNot(dto.getContractNo(), dto.getId())) {
            throw new RuntimeException("合同编号已存在");
        }

        String oldContractNo = contract.getContractNo();
        BeanUtils.copyProperties(dto, contract, "id", "status", "createTime", "attachments", "approvalRecords");
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.UPDATE, "admin",
                "编辑合同" + (!oldContractNo.equals(contract.getContractNo()) ? "，合同编号由 " + oldContractNo + " 变更为 " + contract.getContractNo() : ""));

        return contract;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Contract contract = getDetail(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("仅草稿状态的合同可以删除");
        }
        contractRepository.delete(contract);
    }

    @Override
    @Transactional
    public Contract submitApproval(Long id, String operator) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.REJECTED
                && contract.getStatus() != ContractStatus.WITHDRAWN) {
            throw new RuntimeException("当前状态不允许提交审批");
        }

        if (contract.getAttachments() == null || contract.getAttachments().isEmpty()) {
            throw new RuntimeException("请至少上传一个附件后再提交审批");
        }

        contract.setStatus(ContractStatus.PENDING_APPROVAL);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.SUBMIT, operator, "提交审批");

        return contract;
    }

    @Override
    @Transactional
    public Contract approve(Long id, String operator, String remark) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL) {
            throw new RuntimeException("仅待审批状态的合同可以审批");
        }

        contract.setStatus(ContractStatus.APPROVED);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.APPROVE, operator,
                StringUtils.hasText(remark) ? remark : "审批通过");

        return contract;
    }

    @Override
    @Transactional
    public Contract reject(Long id, String operator, String remark) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL) {
            throw new RuntimeException("仅待审批状态的合同可以审批");
        }

        if (!StringUtils.hasText(remark)) {
            throw new RuntimeException("驳回时必须填写审批意见");
        }

        contract.setStatus(ContractStatus.REJECTED);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.REJECT, operator, remark);

        return contract;
    }

    @Override
    @Transactional
    public Contract withdraw(Long id, String operator) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL) {
            throw new RuntimeException("仅待审批状态的合同可以撤回");
        }

        contract.setStatus(ContractStatus.WITHDRAWN);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.WITHDRAW, operator, "撤回审批");

        return contract;
    }

    @Override
    @Transactional
    public Contract archive(Long id, String operator) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.APPROVED) {
            throw new RuntimeException("仅审批通过的合同可以归档");
        }

        contract.setStatus(ContractStatus.ARCHIVED);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.ARCHIVE, operator, "合同归档");

        return contract;
    }

    private boolean isEditable(ContractStatus status) {
        return status == ContractStatus.DRAFT
                || status == ContractStatus.REJECTED
                || status == ContractStatus.WITHDRAWN;
    }

    private void addApprovalRecord(Contract contract, ApprovalAction action, String operator, String remark) {
        ApprovalRecord record = new ApprovalRecord();
        record.setContract(contract);
        record.setAction(action);
        record.setOperator(operator);
        record.setRemark(remark);
        approvalRecordRepository.save(record);
    }
}
