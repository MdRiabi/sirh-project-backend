package com.sirh.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckOutRequest(@NotNull UUID employeId) {}
