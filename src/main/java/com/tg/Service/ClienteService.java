package com.tg.Service;

import com.tg.DTO.ClienteDTO;
import com.tg.Entities.ClienteEntity;
import com.tg.Repositories.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
 public class ClienteService {

    private final ClienteRepository clienteRepository;


    @org.springframework.transaction.annotation.Transactional
    public ClienteEntity salvar(ClienteDTO dto) {

        if (dto.cpf() != null && clienteRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException(
                    "Já existe um cliente cadastrado com este CPF."
            );
        }

        ClienteEntity cliente = new ClienteEntity();

        cliente.setNome(dto.nome());
        cliente.setDataNascimento(dto.dataNascimento());
        cliente.setCpf(dto.cpf());
        cliente.setRg(dto.rg());
        cliente.setOrgaoExpedidor(dto.orgaoExpedidor());
        cliente.setTelefone(dto.telefone());
        cliente.setEmail(dto.email());
        cliente.setEndereco(dto.endereco());
        cliente.setNumero(dto.numero());
        cliente.setBairro(dto.bairro());
        cliente.setCidade(dto.cidade());
        cliente.setEstado(dto.estado());
        cliente.setCep(dto.cep());

        cliente.setRevisado(true);
        return clienteRepository.save(cliente);
    }

    public org.springframework.data.domain.Page<ClienteEntity> listar(int page) {

        return clienteRepository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50));
    }


    public Optional<ClienteEntity> buscarPorId(Long id) {

        return clienteRepository.findById(id);
    }


    public Optional<ClienteEntity> buscarPorCpf(String cpf) {

        return clienteRepository.findByCpf(cpf);
    }


    @org.springframework.transaction.annotation.Transactional
    public ClienteEntity atualizar(Long id, ClienteDTO dto) {

        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cliente não encontrado."
                        )
                );

        /*
         * Verifica se o CPF informado já pertence
         * a outro cliente.
         */
        if (dto.cpf() != null) {

            Optional<ClienteEntity> clienteComMesmoCpf =
                    clienteRepository.findByCpf(dto.cpf());

            if (clienteComMesmoCpf.isPresent()
                    && !clienteComMesmoCpf.get().getId().equals(id)) {

                throw new IllegalArgumentException(
                        "O CPF informado já pertence a outro cliente."
                );
            }
        }

        cliente.setNome(dto.nome());
        cliente.setDataNascimento(dto.dataNascimento());
        cliente.setCpf(dto.cpf());
        cliente.setRg(dto.rg());
        cliente.setOrgaoExpedidor(dto.orgaoExpedidor());
        cliente.setTelefone(dto.telefone());
        cliente.setEmail(dto.email());
        cliente.setEndereco(dto.endereco());
        cliente.setNumero(dto.numero());
        cliente.setBairro(dto.bairro());
        cliente.setCidade(dto.cidade());
        cliente.setEstado(dto.estado());
        cliente.setCep(dto.cep());

        cliente.setRevisado(true);
        return clienteRepository.save(cliente);
    }


    @org.springframework.transaction.annotation.Transactional
    public void deletar(Long id) {

        if (!clienteRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Cliente não encontrado."
            );
        }

        clienteRepository.deleteById(id);
    }
}