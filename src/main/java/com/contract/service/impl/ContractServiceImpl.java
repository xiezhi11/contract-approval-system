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
import com.contract.security.SecurityUtil;
import com.contract.service.ContractService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
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

    @Autowired
    private SecurityUtil securityUtil;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_APPROVER = "APPROVER";
    private static final String ROLE_ARCHIVIST = "ARCHIVIST";
    private static final String ROLE_USER = "USER";

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
                Predicate signDatePredicate = cb.greaterThanOrEqualTo(root.get("signDate"), queryDTO.getStartDate());
                Predicate effectiveDatePredicate = cb.greaterThanOrEqualTo(root.get("effectiveDate"), queryDTO.getStartDate());
                predicates.add(cb.or(signDatePredicate, effectiveDatePredicate));
            }
            if (queryDTO.getEndDate() != null) {
                Predicate signDatePredicate = cb.lessThanOrEqualTo(root.get("signDate"), queryDTO.getEndDate());
                Predicate effectiveDatePredicate = cb.lessThanOrEqualTo(root.get("effectiveDate"), queryDTO.getEndDate());
                predicates.add(cb.or(signDatePredicate, effectiveDatePredicate));
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

        validateContractDates(dto);

        String currentUser = securityUtil.getCurrentUsername();

        Contract contract = new Contract();
        BeanUtils.copyProperties(dto, contract);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setCreator(currentUser);
        if (!StringUtils.hasText(contract.getResponsiblePerson())) {
            contract.setResponsiblePerson(currentUser);
        }
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.CREATE, currentUser, "创建合同");

        return contract;
    }

    @Override
    @Transactional
    public Contract update(ContractDTO dto) {
        Contract contract = getDetail(dto.getId());

        if (!isEditable(contract.getStatus())) {
            throw new RuntimeException("当前状态不允许编辑");
        }

        String currentUser = securityUtil.getCurrentUsername();
        if (!canEditContract(contract, currentUser)) {
            throw new AccessDeniedException("没有权限编辑该合同");
        }

        if (contractRepository.existsByContractNoAndIdNot(dto.getContractNo(), dto.getId())) {
            throw new RuntimeException("合同编号已存在");
        }

        validateContractDates(dto);

        String oldContractNo = contract.getContractNo();
        BeanUtils.copyProperties(dto, contract, "id", "status", "createTime", "attachments", "approvalRecords", "creator");
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.UPDATE, currentUser,
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

        String currentUser = securityUtil.getCurrentUsername();
        if (!canEditContract(contract, currentUser)) {
            throw new AccessDeniedException("没有权限删除该合同");
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

        String currentUser = securityUtil.getCurrentUsername();
        if (!canEditContract(contract, currentUser)) {
            throw new AccessDeniedException("没有权限提交该合同");
        }

        contract.setStatus(ContractStatus.PENDING_APPROVAL);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.SUBMIT, currentUser, "提交审批");

        return contract;
    }

    @Override
    @Transactional
    public Contract approve(Long id, String operator, String remark) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL) {
            throw new RuntimeException("仅待审批状态的合同可以审批");
        }

        String currentUser = securityUtil.getCurrentUsername();
        if (!canApprove(currentUser)) {
            throw new AccessDeniedException("没有审批权限");
        }

        contract.setStatus(ContractStatus.APPROVED);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.APPROVE, currentUser,
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

        String currentUser = securityUtil.getCurrentUsername();
        if (!canApprove(currentUser)) {
            throw new AccessDeniedException("没有审批权限");
        }

        contract.setStatus(ContractStatus.REJECTED);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.REJECT, currentUser, remark);

        return contract;
    }

    @Override
    @Transactional
    public Contract withdraw(Long id, String operator) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL) {
            throw new RuntimeException("仅待审批状态的合同可以撤回");
        }

        String currentUser = securityUtil.getCurrentUsername();
        if (!canEditContract(contract, currentUser)) {
            throw new AccessDeniedException("没有权限撤回该合同");
        }

        contract.setStatus(ContractStatus.WITHDRAWN);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.WITHDRAW, currentUser, "撤回审批");

        return contract;
    }

    @Override
    @Transactional
    public Contract archive(Long id, String operator) {
        Contract contract = getDetail(id);

        if (contract.getStatus() != ContractStatus.APPROVED) {
            throw new RuntimeException("仅审批通过的合同可以归档");
        }

        String currentUser = securityUtil.getCurrentUsername();
        if (!canArchive(currentUser)) {
            throw new AccessDeniedException("没有归档权限");
        }

        contract.setStatus(ContractStatus.ARCHIVED);
        contract = contractRepository.save(contract);

        addApprovalRecord(contract, ApprovalAction.ARCHIVE, currentUser, "合同归档");

        return contract;
    }

    private boolean isEditable(ContractStatus status) {
        return status == ContractStatus.DRAFT
                || status == ContractStatus.REJECTED
                || status == ContractStatus.WITHDRAWN;
    }

    private boolean canEditContract(Contract contract, String currentUser) {
        if (securityUtil.hasRole(ROLE_ADMIN)) {
            return true;
        }
        return contract.getCreator().equals(currentUser)
                || (contract.getResponsiblePerson() != null && contract.getResponsiblePerson().equals(currentUser));
    }

    private boolean canApprove(String currentUser) {
        return securityUtil.hasRole(ROLE_ADMIN) || securityUtil.hasRole(ROLE_APPROVER);
    }

    private boolean canArchive(String currentUser) {
        return securityUtil.hasRole(ROLE_ADMIN) || securityUtil.hasRole(ROLE_ARCHIVIST);
    }

    private void addApprovalRecord(Contract contract, ApprovalAction action, String operator, String remark) {
        ApprovalRecord record = new ApprovalRecord();
        record.setContract(contract);
        record.setAction(action);
        record.setOperator(operator);
        record.setRemark(remark);
        approvalRecordRepository.save(record);
    }

    private void validateContractDates(ContractDTO dto) {
        if (dto.getSignDate() != null && dto.getEffectiveDate() != null) {
            if (dto.getEffectiveDate().isBefore(dto.getSignDate())) {
                throw new RuntimeException("生效日期不能早于签订日期");
            }
        }
        if (dto.getEffectiveDate() != null && dto.getExpiryDate() != null) {
            if (dto.getExpiryDate().isBefore(dto.getEffectiveDate())) {
                throw new RuntimeException("到期日期不能早于生效日期");
            }
        }
        if (dto.getSignDate() != null && dto.getExpiryDate() != null) {
            if (dto.getExpiryDate().isBefore(dto.getSignDate())) {
                throw new RuntimeException("到期日期不能早于签订日期");
            }
        }
    }
}
