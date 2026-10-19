package com.fms.web;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Translates the short messages shown after saving ("Payment recorded.").
 * They are written in English in the code; the translation is found under
 * "flash." + a key made from the English text in messages_*.properties.
 * Messages that include names or numbers have no entry and stay as they are.
 * Used in templates as ${@tr.t(message)}.
 */
@Component("tr")
public class FlashText {

    private final MessageSource messages;

    public FlashText(MessageSource messages) {
        this.messages = messages;
    }

    public String t(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        Locale locale = LocaleContextHolder.getLocale();
        return messages.getMessage("flash." + slug(text), null, text, locale);
    }

    /** Same rule as the generator: ASCII letters/digits, everything else "_", max 60 chars. */
    static String slug(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "");
        String s = ascii.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+|_+$", "").toLowerCase(Locale.ROOT);
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
