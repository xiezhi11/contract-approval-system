package com.contract.service;

import com.contract.entity.ContractAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AttachmentService {
    ContractAttachment upload(Long contractId, MultipartFile file, String uploader) throws IOException;
    void delete(Long id);
    List<ContractAttachment> getByContractId(Long contractId);
}
