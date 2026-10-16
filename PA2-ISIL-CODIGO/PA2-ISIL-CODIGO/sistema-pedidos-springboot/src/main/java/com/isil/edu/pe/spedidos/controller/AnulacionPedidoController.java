package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.controller.dto.AnulacionPedidoRequest;
import com.isil.edu.pe.spedidos.controller.dto.AnulacionPedidoResponse;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/anulaciones")
public class AnulacionPedidoController {
  private final AnulacionPedidoService service;

  public AnulacionPedidoController(AnulacionPedidoService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AnulacionPedidoResponse anular(@Valid @RequestBody AnulacionPedidoRequest request) {
    return AnulacionPedidoResponse.from(service.anular(request.pedidoId(), request.motivo()));
  }

  @GetMapping
  public List<AnulacionPedidoResponse> listar() {
    return service.listarAnulaciones().stream().map(AnulacionPedidoResponse::from).toList();
  }
}
