package com.nanobank.ledger.wallet.application.dto;

import com.nanobank.ledger.wallet.domain.model.WalletCategory;
import jakarta.validation.constraints.Size;

public record UpdateWalletRequest(
        @Size(max = 100) String name,
        WalletCategory category,
        @Size(max = 500) String description
) {}
