package com.alejandro.banking_api.repository;


import com.alejandro.banking_api.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    //search all transfers of users (source and destination transfer)
    List<Transfer> findBySourceAccountUserIdOrDestinationAccountUserIdOrderByCreatedAtDesc(
            Long sourceUserId,
            Long destinationUserId
    );
}
