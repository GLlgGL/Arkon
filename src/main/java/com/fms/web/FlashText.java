package com.fms.web;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Translates the short messages shown after saving ("Payment recorded.").
 * They are written in English in the code; the translation is found under
 * "flash." + a key made from the English text in messages_*.properties.
 * Messages that include names or numbers are matched against patterns
 * (flashp.* keys) and translated with those values put back in.
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
        String exact = messages.getMessage("flash." + slug(text), null, null, locale);
        if (exact != null) {
            return exact;
        }
        for (Pat p : PATTERNS) {
            Matcher m = p.regex.matcher(text.trim());
            if (m.matches()) {
                Object[] args = new Object[m.groupCount()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = m.group(i + 1);
                }
                return messages.getMessage("flashp." + p.key, args, text, locale);
            }
        }
        return text;
    }

    private record Pat(String key, Pattern regex) {
        Pat(String key, String regex) {
            this(key, Pattern.compile(regex));
        }
    }

    /** Messages with names or numbers in them; most specific first. */
    private static final List<Pat> PATTERNS = List.of(
        new Pat("handedOver", "Records handed over: (\\d+) lines\\."),
        new Pat("batchNoStock", "Batch (.+?) of (.+?) has no stock here\\."),
        new Pat("batchExpired", "Batch (.+?) of (.+?) has expired\\."),
        new Pat("batchOnly", "Batch (.+?) has only (\\d+) of (.+?)\\."),
        new Pat("noSingleBatch", "No single batch of (.+?) has (\\d+) that haven't expired\\. Split the quantity into lines per batch\\."),
        new Pat("enterSerials", "Enter (\\d+) serial numbers for (.+?) \\(one per unit\\); you entered (\\d+)\\."),
        new Pat("serialExists", "Serial number (.+?) of (.+?) already exists\\."),
        new Pat("serialNotHere", "Serial number (.+?) of (.+?) isn't in stock here\\."),
        new Pat("serialsOnly", "Only (\\d+) serial numbers of (.+?) are in stock here\\."),
        new Pat("serialsSold", "Some serial numbers of (.+?) from this purchase were already sold, so the line can't be removed\\."),
        new Pat("enterBatch", "Enter the batch number of (.+?)\\."),
        new Pat("transferSaved", "Transfer (TR-\\d+) saved\\."),
        new Pat("notEnoughAtSource", "Not enough stock at the sending location: (.+?)\\."),
        new Pat("soldAtDest", "Some of the goods have already been sold or moved at (.+?), so this transfer can't be undone\\."),
        new Pat("unknownProduct", "Unknown product: (.+)"),
        new Pat("posReceived", "The amount received is less than the total \\((.+?)\\)\\."),
        new Pat("rateMissing", "Enter the (EUR|USD) exchange rate first \\(Administration → Exchange rates\\)\\."),
        new Pat("lockedPeriod", "This date is in a locked period \\(up to (\\S+)\\)\\. Unlock it in Settings to make changes\\."),
        new Pat("countApplied", "Stock count applied: (\\d+) corrections\\."),
        new Pat("remindersSent", "Reminders sent: (\\d+)\\."),
        new Pat("yearClosed", "Year (\\d{4}) closed\\. Result: (.+?)\\."),
        new Pat("countShelves", "These products are on shelves, so remove the missing quantity from the right shelf in Stock adjustments: (.+?)\\."),
        new Pat("refundTooMuch", "That's more than the (.+?) to refund on this order\\."),
        new Pat("payTooMuch", "That's more than the (.+?) still to pay on this order\\."),
        new Pat("poAlready", "This purchase order is already (received|cancelled|open)\\."),
        new Pat("payrollExists", "There's already a payroll for (\\d{4}-\\d{2})\\."),
        new Pat("mailFailed", "The email couldn't be sent: (.+?)\\. Check the server, port, security, username and password in Email settings\\."),
        new Pat("accMissing", "Account for (.+?) is missing in the chart of accounts\\."),
        new Pat("unbalanced", "Debits \\((.+?)\\) and credits \\((.+?)\\) must be equal\\."),
        new Pat("prodAlready", "This production order is already (done|cancelled|draft)\\."),
        new Pat("noComponents", "Not enough components: (.+?)\\."),
        new Pat("noBom", "(.+?) has no bill of materials yet\\. Add its components first\\."),
        new Pat("chooseShelfTake", "Choose the shelf to take (.+?) from: only (\\S+) (\\S+) are not on a shelf\\. On shelves: (.+?)\\."),
        new Pat("notOnShelf", "(.+?) isn't on that shelf\\."),
        new Pat("shelfOnly", "Shelf (\\S+) has only (\\S+) (\\S+) of (.+?)\\."),
        new Pat("notOnShelfQty", "Only (\\S+) (\\S+) of (.+?) are not on a shelf\\."),
        new Pat("shelfExists", "Shelf (.+?) already exists\\."),
        new Pat("unknownParty", "Unknown (merchant|company|client): (.+)"),
        new Pat("returnOnly", "Only (\\S+) (\\S+) of (.+?) can still be returned\\."),
        new Pat("returnStock", "Only (\\S+) of (.+?) are in stock, so (\\S+) can't be returned to the merchant\\."),
        new Pat("offerConverted", "This offer was already turned into sale (\\S+)\\."),
        new Pat("offerNoStock", "Not enough stock to turn this offer into a sale: (.+?)\\."),
        new Pat("stockRemove", "Only (\\S+) (\\S+) of (.+?) are in stock, so (\\S+) can't be removed\\."),
        new Pat("stockUndo", "Can't undo: only (\\S+) of (.+?) are left in stock, and this adjustment added (\\S+)\\."),
        new Pat("backupVersion", "This backup is from database version (\\S+), but this app is at version (\\S+)\\. Restore it with the matching app version\\."),
        new Pat("userDeleted", "User (.+?) deleted\\."),
        new Pat("poCreated", "Purchase order (\\S+) created\\."),
        new Pat("goodsReceived", "Goods received: purchase (\\S+) created and stock updated\\."),
        new Pat("poEmailed", "Purchase order emailed to (.+?)\\."),
        new Pat("shelfSaved", "Shelf (\\S+) saved\\."),
        new Pat("accSynced", "Automatic entries updated: (\\d+)\\."),
        new Pat("deletedName", "Deleted (.+?)\\."),
        new Pat("lineAddedSale", "Product added\\. This sale is number (\\S+)\\."),
        new Pat("lineAddedPurchase", "Product added\\. This purchase is number (\\S+)\\."),
        new Pat("lineAddedOffer", "Product added\\. This offer is number (\\S+)\\."),
        new Pat("offerToSale", "Offer (\\S+) is now sale (\\S+), with the offered prices\\."),
        new Pat("invoiceEmailed", "Invoice emailed to (.+?)\\."),
        new Pat("stockSaved", "Saved: (\\S+) (.+?) \\((.+?)\\)\\."),
        new Pat("classExists", "A class called (.+?) already exists\\."),
        new Pat("classSaved", "Class (.+?) saved\\."),
        new Pat("auditCleared", "Audit log cleared \\((\\d+) entries\\)\\."),
        new Pat("reconcileDiff", "Difference of (.+?) recorded as a correction\\.")
    );

    /** Same rule as the generator: ASCII letters/digits, everything else "_", max 60 chars. */
    static String slug(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "");
        String s = ascii.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+|_+$", "").toLowerCase(Locale.ROOT);
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
