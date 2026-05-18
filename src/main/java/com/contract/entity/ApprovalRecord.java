package com.contract.entity;

import com.contract.enums.ApprovalAction;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "approval_record")
public class ApprovalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonIgnore
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ApprovalAction action;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Transient
    private String actionDescription;

    @PostLoad
    public void postLoad() {
        if (this.action != null) {
            this.actionDescription = this.action.getDescription();
        }
    }
}
