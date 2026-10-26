package com.tg.Service;

import com.tg.DTO.ClienteDTO;
import com.tg.Entities.ClienteEntity;
import com.tg.Repositories.ClienteRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClienteService {
  private final ClienteRepository clienteRepository;
  private final AuditoriaService auditoria;

  @Transactional
  public ClienteEntity salvar(ClienteDTO dto) {
    return gravar(new ClienteEntity(), dto);
  }

  @Transactional
  public ClienteEntity atualizar(Long id, ClienteDTO dto) {
    return gravar(
        clienteRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado")),
        dto);
  }

  private ClienteEntity gravar(ClienteEntity c, ClienteDTO d) {
    String cpf = DadosDocumento.cpf(d.cpf());
    clienteRepository
        .findByCpf(cpf)
        .filter(outro -> !Objects.equals(outro.getId(), c.getId()))
        .ifPresent(
            outro -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
            });
    c.setNome(d.nome());
    c.setCpf(cpf);
    c.setRg(d.rg());
    c.setFiliacao(d.filiacao());
    c.setDataNascimento(d.dataNascimento());
    c.setOrgaoExpedidor(d.orgaoExpedidor());
    // O formulário atual não envia os campos complementares: null preserva o valor existente.
    if (d.telefone() != null) c.setTelefone(d.telefone());
    if (d.email() != null) c.setEmail(d.email());
    if (d.endereco() != null) c.setEndereco(d.endereco());
    if (d.numero() != null) c.setNumero(d.numero());
    if (d.bairro() != null) c.setBairro(d.bairro());
    if (d.cidade() != null) c.setCidade(d.cidade());
    if (d.estado() != null) c.setEstado(d.estado());
    if (d.cep() != null) c.setCep(d.cep());
    c.setRevisado(true);
    clienteRepository.saveAndFlush(c);
    auditoria.registrar("CLIENTES", "SALVAR_REVISAO", c.getId().toString());
    return c;
  }

  public List<ClienteEntity> listar() {
    return clienteRepository.findAll();
  }

  public Optional<ClienteEntity> buscarPorId(Long id) {
    return clienteRepository.findById(id);
  }

  public Optional<ClienteEntity> buscarPorCpf(String cpf) {
    return clienteRepository.findByCpf(DadosDocumento.cpf(cpf));
  }

  @Transactional
  public void deletar(Long id) {
    ClienteEntity c =
        clienteRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    clienteRepository.delete(c);
    clienteRepository.flush();
    auditoria.registrar("CLIENTES", "EXCLUIR", id.toString());
  }
}
