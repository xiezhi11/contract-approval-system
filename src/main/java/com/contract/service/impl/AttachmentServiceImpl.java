package com.contract.service.impl;

import com.contract.entity.ApprovalRecord;
import com.contract.entity.Contract;
import com.contract.entity.ContractAttachment;
import com.contract.enums.ApprovalAction;
import com.contract.enums.ContractStatus;
import com.contract.repository.ApprovalRecordRepository;
import com.contract.repository.ContractAttachmentRepository;
import com.contract.repository.ContractRepository;
import com.contract.service.AttachmentService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Value("${contract.upload.path}")
    private String uploadPath;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractAttachmentRepository attachmentRepository;

    @Autowired
    private ApprovalRecordRepository approvalRecordRepository;

    @Override
    @Transactional
    public ContractAttachment upload(Long contractId, MultipartFile file, String uploader) throws IOException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (!isEditable(contract.getStatus())) {
            throw new RuntimeException("当前状态不允许上传附件");
        }

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String relativePath = datePath + "/" + newFileName;

        File targetFile = new File(uploadPath + relativePath);
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        file.transferTo(targetFile);

        String md5 = DigestUtils.md5Hex(file.getInputStream());

        ContractAttachment attachment = new ContractAttachment();
        attachment.setContract(contract);
        attachment.setFileName(originalFileName);
        attachment.setFilePath(relativePath);
        attachment.setFileSize(file.getSize());
        attachment.setMd5(md5);
        attachment.setFileType(extension.substring(1));
        attachment.setUploader(uploader);
        attachment = attachmentRepository.save(attachment);

        addApprovalRecord(contract, ApprovalAction.UPLOAD_ATTACHMENT, uploader, "上传附件：" + originalFileName);

        return attachment;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ContractAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("附件不存在"));

        Contract contract = attachment.getContract();
        if (!isEditable(contract.getStatus())) {
            throw new RuntimeException("当前状态不允许删除附件");
        }

        File file = new File(uploadPath + attachment.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        attachmentRepository.delete(attachment);

        addApprovalRecord(contract, ApprovalAction.DELETE_ATTACHMENT, "admin", "删除附件：" + attachment.getFileName());
    }

    @Override
    public List<ContractAttachment> getByContractId(Long contractId) {
        return attachmentRepository.findByContractIdOrderByCreateTimeDesc(contractId);
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
