package com.tg.Controller;

import com.tg.DTO.ClienteDTO;
import com.tg.Entities.ClienteEntity;
import com.tg.Service.ClienteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

  private final ClienteService clienteService;

  @PostMapping
  public ResponseEntity<ClienteEntity> criar(
      @jakarta.validation.Valid @RequestBody ClienteDTO dto) {

    ClienteEntity cliente = clienteService.salvar(dto);

    return ResponseEntity.ok(cliente);
  }

  @GetMapping
  public ResponseEntity<List<ClienteEntity>> listar(@RequestParam(defaultValue = "") String busca) {

    return ResponseEntity.ok(
        clienteService.listar().stream()
            .filter(
                c -> {
                  String termo = busca.toLowerCase(java.util.Locale.ROOT).trim();
                  return termo.isEmpty()
                      || String.valueOf(c.getNome())
                          .toLowerCase(java.util.Locale.ROOT)
                          .contains(termo)
                      || String.valueOf(c.getCpf())
                          .contains(
                              termo.replaceAll("[^0-9]", "").isEmpty()
                                  ? termo
                                  : termo.replaceAll("[^0-9]", ""))
                      || String.valueOf(c.getTelefone()).contains(termo);
                })
            .toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ClienteEntity> buscarPorId(@PathVariable Long id) {

    return clienteService
        .buscarPorId(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/cpf/{cpf}")
  public ResponseEntity<ClienteEntity> buscarPorCpf(@PathVariable String cpf) {

    return clienteService
        .buscarPorCpf(cpf)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  public ResponseEntity<ClienteEntity> atualizar(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody ClienteDTO dto) {

    ClienteEntity cliente = clienteService.atualizar(id, dto);

    return ResponseEntity.ok(cliente);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletar(@PathVariable Long id) {

    clienteService.deletar(id);

    return ResponseEntity.noContent().build();
  }
}
