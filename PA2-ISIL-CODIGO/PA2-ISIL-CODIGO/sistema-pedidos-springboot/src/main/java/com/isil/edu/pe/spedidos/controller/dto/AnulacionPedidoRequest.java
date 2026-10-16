package com.isil.edu.pe.spedidos.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AnulacionPedidoRequest(
    @NotNull @Positive Long pedidoId,
    @Size(max = 500) String motivo
) {}
