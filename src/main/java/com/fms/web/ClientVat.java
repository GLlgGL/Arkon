package com.fms.web;

import com.fms.domain.Client;
import com.fms.repo.ClientRepository;
import org.springframework.stereotype.Component;

/** For the client page: the client's own VAT settings (${@clientVat.of(id)}). */
@Component("clientVat")
public class ClientVat {

    private final ClientRepository clients;

    public ClientVat(ClientRepository clients) {
        this.clients = clients;
    }

    public Client of(Long id) {
        return id == null ? null : clients.findById(id).orElse(null);
    }
}
