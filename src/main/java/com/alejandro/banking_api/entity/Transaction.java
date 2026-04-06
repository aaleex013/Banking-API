package com.alejandro.banking_api.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    //type debit(income) or credit(expense)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    //account associated with this transaction
    //Account can have multiple transactions
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    //transfer that gave born this movement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
