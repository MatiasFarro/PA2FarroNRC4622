package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AnulacionPedidoServiceImplTest {
  @Mock PedidoRepository pedidoRepository;
  @Mock AnulacionPedidoRepository anulacionRepository;
  @Mock DevolucionStockClient devolucionStockClient;

  private AnulacionPedidoServiceImpl service;
  private Pedido pedido;

  @BeforeEach
  void setUp() {
    service = new AnulacionPedidoServiceImpl(pedidoRepository, anulacionRepository, devolucionStockClient);
    pedido = new Pedido(1L, "Ana", "LAP-001", "Laptop", new BigDecimal("100.00"),
        2, new BigDecimal("200.00"), LocalDateTime.now());
  }

  @Test
  void anulacionCorrectaDevuelveStockMarcaPedidoYGuardaAuditoria() {
    when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
    when(anulacionRepository.existsByPedidoId(1L)).thenReturn(false);
    when(anulacionRepository.save(any(AnulacionPedido.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AnulacionPedido result = service.anular(1L, "Solicitud del cliente");

    verify(devolucionStockClient, times(1)).devolverStock("LAP-001", 2);
    verify(pedidoRepository).save(pedido);
    verify(anulacionRepository).save(any(AnulacionPedido.class));
    assertEquals(PedidoEstado.ANULADO, pedido.getEstado());
    assertEquals(1L, result.getPedidoId());
    assertEquals(2, result.getCantidadDevuelta());
  }

  @Test
  void anulacionDuplicadaSeRechazaSinNuevaDevolucion() {
    pedido.setEstado(PedidoEstado.ANULADO);
    when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
    when(anulacionRepository.existsByPedidoId(1L)).thenReturn(true);

    assertThrows(ResponseStatusException.class, () -> service.anular(1L, "duplicada"));

    verifyNoInteractions(devolucionStockClient);
    verify(anulacionRepository, never()).save(any());
    verify(pedidoRepository, never()).save(any());
  }

  @Test
  void falloDelInventarioNoGuardaAnulacionNiConfirmaCambioLocal() {
    when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
    when(anulacionRepository.existsByPedidoId(1L)).thenReturn(false);
    when(devolucionStockClient.devolverStock("LAP-001", 2))
        .thenThrow(new IllegalStateException("inventario apagado"));

    assertThrows(ResponseStatusException.class, () -> service.anular(1L, "prueba"));

    assertEquals(PedidoEstado.REGISTRADO, pedido.getEstado());
    verify(pedidoRepository, never()).save(any());
    verify(anulacionRepository, never()).save(any());
  }
}
