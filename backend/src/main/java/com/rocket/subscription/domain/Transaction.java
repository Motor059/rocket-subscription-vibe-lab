package com.rocket.subscription.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions",
        indexes = {
                // 성능 향상: 특정 유저의 최근 결제 내역을 날짜순/가맹점별로 빠르게 쿼리하기 위한 복합 인덱스 설정
                @Index(name = "idx_user_date_merchant", columnList = "user_id, transactionDate, merchantName")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    // 결제 내역 생성을 위한 정적 팩토리 메서드
    public static Transaction create(User user, String merchantName, int amount, LocalDateTime transactionDate) {
        Transaction transaction = new Transaction();
        transaction.user = user;
        transaction.merchantName = merchantName;
        transaction.amount = amount;
        transaction.transactionDate = transactionDate;
        return transaction;
    }
}