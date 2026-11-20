package com.merida.tecnm.mx.market.backend.persistence;

import com.merida.tecnm.mx.market.backend.domain.Client;
import com.merida.tecnm.mx.market.backend.domain.repository.ClientRepository;
import com.merida.tecnm.mx.market.backend.persistence.crud.ClienteCrudRepository;
import com.merida.tecnm.mx.market.backend.persistence.entity.Cliente;
import com.merida.tecnm.mx.market.backend.persistence.mapper.ClientMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepositoryAdapter implements ClientRepository {
    private final ClienteCrudRepository clienteCrudRepository;
    private final ClientMapper mapper;

    public ClientRepositoryAdapter(ClienteCrudRepository clienteCrudRepository, ClientMapper mapper) {
        this.clienteCrudRepository = clienteCrudRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Client> getAll() {
        List<Cliente> clientes = (List<Cliente>) clienteCrudRepository.findAll();
        return mapper.toClients(clientes);
    }

    @Override
    public Optional<Client> getClient(int clientId) {
        return clienteCrudRepository.findById(clientId).map(mapper::toClient);
    }

    @Override
    public Client save(Client client) {
        Cliente cliente = mapper.toCliente(client);
        return mapper.toClient(clienteCrudRepository.save(cliente));
    }

    @Override
    public void delete(int clientId) {
        clienteCrudRepository.deleteById(clientId);
    }
}