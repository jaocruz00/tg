package com.tg.Controller;

import com.tg.DTO.ClienteDTO;
import com.tg.Entities.ClienteEntity;
import com.tg.Service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ClienteController {


    private final ClienteService clienteService;


    @PostMapping
    public ResponseEntity<ClienteEntity> criar(
            @RequestBody ClienteDTO dto) {

        ClienteEntity cliente = clienteService.salvar(dto);

        return ResponseEntity.ok(cliente);
    }


    @GetMapping
    public ResponseEntity<List<ClienteEntity>> listar() {

        return ResponseEntity.ok(
                clienteService.listar()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ClienteEntity> buscarPorId(
            @PathVariable Long id) {

        return clienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<ClienteEntity> buscarPorCpf(
            @PathVariable String cpf) {

        return clienteService.buscarPorCpf(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public ResponseEntity<ClienteEntity> atualizar(
            @PathVariable Long id,
            @RequestBody ClienteDTO dto) {

        ClienteEntity cliente = clienteService.atualizar(id, dto);

        return ResponseEntity.ok(cliente);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        clienteService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}
