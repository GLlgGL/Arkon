package com.fms.service;

import com.fms.domain.OrderType;
import com.fms.domain.Party;
import com.fms.domain.Prospect;
import com.fms.repo.ClientRepository;
import com.fms.repo.CompanyRepository;
import com.fms.repo.MerchantRepository;
import com.fms.repo.ProspectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who can be the provider and the buyer of each order type:
 *   Purchase: Merchant -> Company
 *   Sale:     Company  -> Client
 *   Offer:    Company  -> Client, or a new prospect (was NotClients)
 */
@Service
public class PartyService {

    private final MerchantRepository merchants;
    private final CompanyRepository companies;
    private final ClientRepository clients;
    private final ProspectRepository prospects;

    public PartyService(MerchantRepository merchants, CompanyRepository companies,
                        ClientRepository clients, ProspectRepository prospects) {
        this.merchants = merchants;
        this.companies = companies;
        this.clients = clients;
        this.prospects = prospects;
    }

    @Transactional(readOnly = true)
    public List<String> providerNames(OrderType type) {
        List<? extends Party> list = type == OrderType.PURCHASE
                ? merchants.findAllByOrderByNameAsc()
                : companies.findAllByOrderByNameAsc();
        return list.stream().map(Party::getName).toList();
    }

    @Transactional(readOnly = true)
    public List<String> buyerNames(OrderType type) {
        List<? extends Party> list = type == OrderType.PURCHASE
                ? companies.findAllByOrderByNameAsc()
                : clients.findAllByOrderByNameAsc();
        return list.stream().map(Party::getName).toList();
    }

    @Transactional(readOnly = true)
    public boolean isValidProvider(OrderType type, String name) {
        return type == OrderType.PURCHASE ? merchants.existsByName(name) : companies.existsByName(name);
    }

    @Transactional(readOnly = true)
    public boolean isValidBuyer(OrderType type, String name) {
        return type == OrderType.PURCHASE ? companies.existsByName(name) : clients.existsByName(name);
    }

    @Transactional(readOnly = true)
    public boolean isClient(String name) {
        return name != null && clients.existsByName(name);
    }

    @Transactional(readOnly = true)
    public PartyInfo provider(OrderType type, String name) {
        var found = type == OrderType.PURCHASE
                ? merchants.findByName(name).map(PartyInfo::of)
                : companies.findByName(name).map(PartyInfo::of);
        return found.orElseGet(() -> PartyInfo.unknown(name));
    }

    @Transactional(readOnly = true)
    public PartyInfo buyer(OrderType type, String name) {
        if (type == OrderType.PURCHASE) {
            return companies.findByName(name).map(PartyInfo::of).orElseGet(() -> PartyInfo.unknown(name));
        }
        var client = clients.findByName(name).map(PartyInfo::of);
        if (client.isPresent() || type != OrderType.OFFER) {
            return client.orElseGet(() -> PartyInfo.unknown(name));
        }
        // Offer to someone who is not a client: details live in PROSPECT.
        return prospects.findFirstByNameOrderByRegisteredAtDesc(name)
                .map(p -> new PartyInfo(p.getName(), p.getTin(), p.getAddress(), p.getEmail(), p.getPhone(), false))
                .orElseGet(() -> PartyInfo.unknown(name));
    }

    /** Registers an offer recipient as a client, using the details saved with the offer. */
    @Transactional
    public void registerClientFromProspect(String name) {
        if (clients.existsByName(name)) {
            return;
        }
        com.fms.domain.Client c = new com.fms.domain.Client();
        c.setName(name);
        prospects.findFirstByNameOrderByRegisteredAtDesc(name).ifPresent(p -> {
            c.setTin(p.getTin());
            c.setAddress(p.getAddress());
            c.setEmail(p.getEmail());
            c.setPhone(p.getPhone());
        });
        clients.save(c);
    }

    /**
     * Records an offer recipient who is not a client. The original inserted a
     * NotClients row for every offer line; this keeps one row per name.
     */
    @Transactional
    public void saveProspect(String name, String tin, String address, String email, String phone) {
        Prospect p = prospects.findFirstByNameOrderByRegisteredAtDesc(name).orElseGet(Prospect::new);
        p.setName(name);
        p.setTin(tin);
        p.setAddress(address);
        p.setEmail(email);
        p.setPhone(phone);
        prospects.save(p);
    }
}
