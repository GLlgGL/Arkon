package com.fms.service;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Texts IMS writes into records (journal entry descriptions, movement
 * descriptions). Like invoices, they use the invoice language from Settings,
 * because they're stored as written.
 */
@Component
public class DocText {

    private final MessageSource messages;
    private final ConfigService config;

    public DocText(MessageSource messages, ConfigService config) {
        this.messages = messages;
        this.config = config;
    }

    public Locale locale() {
        String l = config.get().getInvoiceLang();
        return Locale.forLanguageTag(l == null || l.isBlank() ? "sq" : l);
    }

    public String t(String key, String fallback, Object... args) {
        return messages.getMessage(key, args, fallback, locale());
    }
}
