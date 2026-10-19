package com.fms.service;

import com.fms.domain.Party;

/** Name and contact details printed on an invoice and shown on the order page. */
public record PartyInfo(String name, String tin, String address, String email, String phone, boolean registered) {

    public static PartyInfo of(Party p) {
        return new PartyInfo(p.getName(), p.getTin(), p.getAddress(), p.getEmail(), p.getPhone(), true);
    }

    /** A name that is no longer in the registry (renamed or deleted since the order). */
    public static PartyInfo unknown(String name) {
        return new PartyInfo(name, "", "", "", "", false);
    }
}
