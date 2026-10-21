package com.fms.web;

import com.fms.repo.MerchantRepository;
import org.springframework.stereotype.Component;

/** For the supplier page: the default purchase type (${@merchantKind.of(id)}). */
@Component("merchantKind")
public class MerchantKind {

    private final MerchantRepository merchants;

    public MerchantKind(MerchantRepository merchants) {
        this.merchants = merchants;
    }

    public String of(Long id) {
        return id == null ? "LOCAL" : merchants.findById(id).map(m -> m.getPurchaseKind()).orElse("LOCAL");
    }
}
