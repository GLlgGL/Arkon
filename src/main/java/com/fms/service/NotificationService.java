package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.ReminderLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifications (the bell) and payment reminders by email.
 * Notifications follow the user's access. Reminders go to clients with an
 * email address: some days before an invoice falls due, then regularly while
 * it's overdue; each reminder is logged so it's never sent twice.
 */
@Service
public class NotificationService {

    public record Note(String level, String text, String href) { }
    public record Candidate(PaymentService.DebtRow debt, String email, String kind) { }

    private final ConfigService config;
    private final AccessService access;
    private final PaymentService payments;
    private final ReportService reports;
    private final TaxService taxes;
    private final PartyService parties;
    private final MailService mail;
    private final ReminderLogRepository log;
    private final MessageSource messages;
    private final DocText doc;
    private final AuditService audit;

    public NotificationService(ConfigService config, AccessService access, PaymentService payments, ReportService reports,
                               TaxService taxes, PartyService parties, MailService mail, ReminderLogRepository log,
                               MessageSource messages, DocText doc, AuditService audit) {
        this.config = config; this.access = access; this.payments = payments; this.reports = reports; this.taxes = taxes;
        this.parties = parties; this.mail = mail; this.log = log; this.messages = messages; this.doc = doc; this.audit = audit;
    }

    private String t(String key, Object... args) {
        return messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    /** What the bell shows for this user. */
    @Transactional(readOnly = true)
    public List<Note> forUser(AppUser u) {
        List<Note> out = new ArrayList<>();
        var lv = access.levels(u);
        LocalDate today = LocalDate.now();
        int soon = Math.max(1, config.get().getRemDaysBefore());
        if (lv.get(Area.SALES).atLeast(Area.Level.VIEW) || lv.get(Area.DEBTS).atLeast(Area.Level.VIEW)) {
            var mine = payments.debts(OrderType.SALE).orders().stream()
                    .filter(r -> u.isSeesAll() || u.getUsername().equals(r.createdBy())).toList();
            var overdue = mine.stream().filter(r -> r.status() == PaymentService.Status.OVERDUE).toList();
            var dueSoon = mine.stream().filter(r -> r.dueDate() != null && !r.dueDate().isBefore(today)
                    && !r.dueDate().isAfter(today.plusDays(soon)) && r.status() != PaymentService.Status.OVERDUE).toList();
            String href = lv.get(Area.DEBTS).atLeast(Area.Level.VIEW) ? "/debts/sales" : "/sales";
            if (!overdue.isEmpty()) out.add(new Note("alert", t("note.overdue", overdue.size(), Money.format(sum(overdue))), href));
            if (!dueSoon.isEmpty()) out.add(new Note("warn", t("note.dueSoon", dueSoon.size(), soon), href));
        }
        if (lv.get(Area.DEBTS).atLeast(Area.Level.VIEW)) {
            var overdue = payments.debts(OrderType.PURCHASE).orders().stream().filter(r -> r.status() == PaymentService.Status.OVERDUE).toList();
            if (!overdue.isEmpty()) out.add(new Note("warn", t("note.weOwe", overdue.size(), Money.format(sum(overdue))), "/debts/purchases"));
        }
        if (lv.get(Area.STOCK).atLeast(Area.Level.VIEW) || lv.get(Area.REPORTS).atLeast(Area.Level.VIEW)) {
            long low = reports.warehouse(Period.custom(reports.firstActivityDate(), today)).stream()
                    .filter(r -> r.quantityLimit() > 0 && r.remaining() < r.quantityLimit()).count();
            if (low > 0) out.add(new Note("warn", t("note.lowStock", low), lv.get(Area.REPORTS).atLeast(Area.Level.VIEW) ? "/reports/warehouse?mode=supply" : "/stock"));
        }
        if (lv.get(Area.TAXES).atLeast(Area.Level.VIEW)) {
            for (var r : taxes.overview()) {
                if ("OVERDUE".equals(r.status()) || ("DUE".equals(r.status()) && !r.dueDate().isAfter(today.plusDays(7)))) {
                    out.add(new Note("OVERDUE".equals(r.status()) ? "alert" : "warn",
                            t("note.tax", t("tx.kind." + r.kind()), r.period(), r.dueDate().format(DateTimeFormatter.ofPattern("dd/MM")), Money.format(r.open())), "/taxes"));
                }
            }
        }
        return out;
    }

    private static BigDecimal sum(List<PaymentService.DebtRow> l) {
        return l.stream().map(PaymentService.DebtRow::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -------------------------------------------------------------- reminders

    /** Reminders that are due today. */
    @Transactional(readOnly = true)
    public List<Candidate> candidates() {
        var c = config.get();
        LocalDate today = LocalDate.now();
        List<Candidate> out = new ArrayList<>();
        for (var r : payments.debts(OrderType.SALE).orders()) {
            if (r.dueDate() == null || r.remaining().signum() <= 0) continue;
            String email;
            try {
                PartyInfo p = parties.buyer(OrderType.SALE, r.party());
                email = p == null ? null : p.email();
            } catch (RuntimeException e) { email = null; }
            if (email == null || email.isBlank()) continue;
            var sent = log.findByOrderCodeOrderBySentAtDesc(r.code());
            if (!today.isAfter(r.dueDate())) {
                boolean window = !today.isBefore(r.dueDate().minusDays(c.getRemDaysBefore()));
                boolean already = sent.stream().anyMatch(s -> "BEFORE".equals(s.getKind()));
                if (window && !already) out.add(new Candidate(r, email, "BEFORE"));
            } else {
                var last = sent.stream().filter(s -> "AFTER".equals(s.getKind())).findFirst();
                if (last.isEmpty() || !last.get().getSentAt().toLocalDate().isAfter(today.minusDays(Math.max(1, c.getRemDaysAfter())))) {
                    out.add(new Candidate(r, email, "AFTER"));
                }
            }
        }
        return out;
    }

    /** Sends today's reminders. Returns how many were sent. */
    @Transactional
    public int sendDue() {
        var c = config.get();
        if (!c.isModReminders() || !c.isMailReady()) {
            throw new OrderException("Switch on email in Administration → Email settings to send reminders.");
        }
        int n = 0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Candidate x : candidates()) {
            var r = x.debt();
            String amount = Money.format(r.remaining()) + " " + c.getCurrency();
            String subject = doc.t("rem.subject." + x.kind(), "Payment reminder: invoice {0}", r.code());
            String body = doc.t("rem.body." + x.kind(), "Invoice {0} of {1} for {2} is due on {3}.", r.code(), r.date().format(f), amount, r.dueDate().format(f))
                    + "\n\n" + doc.t("rem.footer", "If you have already paid, please ignore this message. Thank you.");
            mail.sendText(x.email(), subject, body);
            ReminderLog l = new ReminderLog();
            l.setOrderCode(r.code());
            l.setKind(x.kind());
            l.setSentTo(x.email());
            log.save(l);
            n++;
        }
        audit.log("REMINDERS", "Email", String.valueOf(n), "payment reminders sent");
        return n;
    }

    /** Every morning at 9:00, when automatic reminders are on. */
    @Scheduled(cron = "0 0 9 * * *")
    public void daily() {
        var c = config.get();
        if (c.isModReminders() && c.isRemAuto() && c.isMailReady()) {
            try {
                sendDue();
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(NotificationService.class).warn("Automatic reminders failed: {}", e.toString());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReminderLog> recent() {
        return log.findTop50ByOrderBySentAtDesc();
    }
}
