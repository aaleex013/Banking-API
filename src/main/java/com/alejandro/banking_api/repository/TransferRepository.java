package com.alejandro.banking_api.repository;

import com.alejandro.banking_api.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
