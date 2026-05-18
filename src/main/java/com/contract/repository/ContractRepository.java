package com.contract.repository;

import com.contract.entity.Contract;
import com.contract.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    boolean existsByContractNo(String contractNo);
    boolean existsByContractNoAndIdNot(String contractNo, Long id);
}
