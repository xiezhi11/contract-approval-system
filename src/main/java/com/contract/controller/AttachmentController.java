package com.contract.controller;

import com.contract.common.Result;
import com.contract.entity.ContractAttachment;
import com.contract.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/api/attachment")
@CrossOrigin
public class AttachmentController {

    @Value("${contract.upload.path}")
    private String uploadPath;

    @Autowired
    private AttachmentService attachmentService;

    @PostMapping("/upload")
    public Result<ContractAttachment> upload(
            @RequestParam Long contractId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false, defaultValue = "admin") String uploader) throws IOException {
        return Result.success(attachmentService.upload(contractId, file, uploader));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return Result.success();
    }

    @GetMapping("/contract/{contractId}")
    public Result<List<ContractAttachment>> getByContractId(@PathVariable Long contractId) {
        return Result.success(attachmentService.getByContractId(contractId));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        ContractAttachment attachment = attachmentService.getByContractId(id).stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("附件不存在"));

        File file = new File(uploadPath + attachment.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }

        Resource resource = new FileSystemResource(file);
        String fileName = URLEncoder.encode(attachment.getFileName(), "UTF-8");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }
}
