package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnulacionPedidoServiceImpl implements AnulacionPedidoService {
  private final PedidoRepository pedidoRepository;
  private final AnulacionPedidoRepository anulacionRepository;
  private final DevolucionStockClient devolucionStockClient;

  public AnulacionPedidoServiceImpl(PedidoRepository pedidoRepository,
      AnulacionPedidoRepository anulacionRepository,
      DevolucionStockClient devolucionStockClient) {
    this.pedidoRepository = pedidoRepository;
    this.anulacionRepository = anulacionRepository;
    this.devolucionStockClient = devolucionStockClient;
  }

  @Override
  @Transactional
  public AnulacionPedido anular(Long pedidoId, String motivo) {
    if (pedidoId == null || pedidoId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pedidoId debe ser positivo.");
    }
    Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el pedido " + pedidoId + "."));
    if (pedido.getEstado() == PedidoEstado.ANULADO || anulacionRepository.existsByPedidoId(pedidoId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya fue anulado.");
    }

    // Primero se confirma la devolución remota. Si falla, no se escribe la anulación local.
    try {
      devolucionStockClient.devolverStock(pedido.getProductoCodigo(), pedido.getCantidad());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo devolver el stock; la anulación no se registró. " + e.getMessage(), e);
    }

    pedido.setEstado(PedidoEstado.ANULADO);
    pedidoRepository.save(pedido);
    return anulacionRepository.save(new AnulacionPedido(
        pedido.getId(), pedido.getProductoCodigo(), pedido.getCantidad(), motivo));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AnulacionPedido> listarAnulaciones() {
    return anulacionRepository.findAllByOrderByIdDesc();
  }
}
