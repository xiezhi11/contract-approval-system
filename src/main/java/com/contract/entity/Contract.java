package com.contract.entity;

import com.contract.enums.ContractStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_no", unique = true, nullable = false, length = 50)
    private String contractNo;

    @Column(name = "contract_name", nullable = false, length = 200)
    private String contractName;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "party_a", length = 200)
    private String partyA;

    @Column(name = "party_b", length = 200)
    private String partyB;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "sign_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate signDate;

    @Column(name = "effective_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    @Column(name = "responsible_person", length = 50)
    private String responsiblePerson;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "contract_type", length = 50)
    private String contractType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContractStatus status;

    @Column(name = "creator", length = 50)
    private String creator;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createTime DESC")
    private List<ApprovalRecord> approvalRecords = new ArrayList<>();
}
