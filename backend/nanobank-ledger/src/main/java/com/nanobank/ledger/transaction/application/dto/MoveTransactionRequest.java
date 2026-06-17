package com.nanobank.ledger.transaction.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveTransactionRequest(
        @NotNull UUID destinationWalletId
) {}
