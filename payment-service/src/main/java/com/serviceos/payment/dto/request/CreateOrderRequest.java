package com.serviceos.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(@NotNull UUID jobId) {}
