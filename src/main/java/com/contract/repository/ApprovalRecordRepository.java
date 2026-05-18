package com.contract.repository;

import com.contract.entity.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {
    List<ApprovalRecord> findByContractIdOrderByCreateTimeDesc(Long contractId);
}
