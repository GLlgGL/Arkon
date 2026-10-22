package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.OrderType;
import com.fms.domain.RecordShare;
import com.fms.repo.AppUserRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.RecordShareRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Which records (sales, purchases, offers and what hangs on them: returns, invoices, payments, unpaid)
 * a user sees: everyone's, or their own plus what others shared with them (view only).
 * Users share all their records or one record; an administrator can hand a user's records to another.
 */
@Service("records")
public class RecordService {

    /** What a user may see: everything, or these creators' records plus these single records. */
    public record Visibility(boolean all, Set<String> owners, Map<OrderType, Set<String>> codes) {
        public boolean sees(OrderType t, String code, String createdBy) {
            return all || owners.contains(createdBy) || codes.getOrDefault(t, Set.of()).contains(code);
        }
        /** For database queries: never empty (an empty list isn't valid SQL). */
        public Collection<String> ownersForQuery() { return owners.isEmpty() ? List.of("~") : owners; }
        public Collection<String> codesForQuery(OrderType t) { Set<String> c = codes.getOrDefault(t, Set.of()); return c.isEmpty() ? List.of("~") : c; }
    }

    private final RecordShareRepository shares;
    private final OrderLineRepository lines;
    private final AppUserRepository users;
    private final AuditService audit;

    public RecordService(RecordShareRepository shares, OrderLineRepository lines, AppUserRepository users, AuditService audit) {
        this.shares = shares; this.lines = lines; this.users = users; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Visibility visibility(AppUser u) {
        if (u == null) return new Visibility(false, Set.of(), Map.of());
        if (u.isSeesAll()) return new Visibility(true, Set.of(), Map.of());
        Set<String> owners = new HashSet<>();
        owners.add(u.getUsername());
        Map<OrderType, Set<String>> codes = new EnumMap<>(OrderType.class);
        for (RecordShare s : shares.findByGranteeOrderByCreatedAtDesc(u.getUsername())) {
            if (s.isAllRecords()) owners.add(s.getOwner());
            else codes.computeIfAbsent(s.getOrderType(), k -> new HashSet<>()).add(s.getOrderCode());
        }
        return new Visibility(false, owners, codes);
    }

    public boolean canView(AppUser u, OrderType t, String code, String createdBy) {
        return visibility(u).sees(t, code, createdBy);
    }

    /** Order code -> who created it, for filtering lists that aren't built from order lines. */
    @Transactional(readOnly = true)
    public Map<String, String> creators(OrderType t) {
        Map<String, String> m = new HashMap<>();
        for (Object[] r : lines.creators(t)) m.put((String) r[0], (String) r[1]);
        return m;
    }

    /** The users a "Created by" filter can offer. */
    @Transactional(readOnly = true)
    public List<String> filterUsers(AppUser u) {
        Visibility v = visibility(u);
        List<String> out = new ArrayList<>();
        for (AppUser x : users.findAll()) if (v.all() || v.owners().contains(x.getUsername())) out.add(x.getUsername());
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** Every username (for choosing whom to share with). */
    @Transactional(readOnly = true)
    public List<String> filterUsersAll() {
        List<String> out = new ArrayList<>();
        users.findAll().forEach(x -> out.add(x.getUsername()));
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    // ------------------------------------------------------------------ sharing

    @Transactional(readOnly = true)
    public List<RecordShare> sharedBy(String owner) { return shares.findByOwnerOrderByCreatedAtDesc(owner); }

    @Transactional(readOnly = true)
    public List<RecordShare> sharedWith(String grantee) { return shares.findByGranteeOrderByCreatedAtDesc(grantee); }

    @Transactional(readOnly = true)
    public List<RecordShare> all() { return shares.findAllByOrderByCreatedAtDesc(); }

    @Transactional(readOnly = true)
    public List<RecordShare> ofRecord(OrderType t, String code) { return shares.findByOrderTypeAndOrderCode(t, code); }

    /** Shares one record (type + code) or all of the owner's records (both null) with a user, view only. */
    @Transactional
    public void share(AppUser by, String owner, String grantee, OrderType type, String code, String note) {
        if (grantee == null || grantee.isBlank()) throw new OrderException("Choose a user to share with.");
        if (users.findByUsername(grantee).isEmpty()) throw new OrderException("Choose a user to share with.");
        if (grantee.equals(owner)) throw new OrderException("You can't share records with yourself.");
        if (!by.isAdmin() && !by.getUsername().equals(owner)) throw new OrderException("You can only share your own records.");
        boolean exists = code == null ? shares.existsByOwnerAndGranteeAndOrderCodeIsNull(owner, grantee)
                : shares.existsByOwnerAndGranteeAndOrderTypeAndOrderCode(owner, grantee, type, code);
        if (exists) throw new OrderException("This is already shared with that user.");
        RecordShare s = new RecordShare();
        s.setOwner(owner);
        s.setGrantee(grantee);
        s.setOrderType(code == null ? null : type);
        s.setOrderCode(code);
        s.setNote(note);
        s.setCreatedBy(by.getUsername());
        shares.save(s);
        audit.log("SHARE", "Records", code == null ? owner + " (all)" : type + " " + code, "view for " + grantee);
    }

    /** Ends a share: the owner, the person it was shared with, or an administrator. */
    @Transactional
    public void revoke(AppUser by, Long id) {
        RecordShare s = shares.findById(id).orElseThrow(() -> new OrderException("Share not found."));
        if (!by.isAdmin() && !by.getUsername().equals(s.getOwner()) && !by.getUsername().equals(s.getGrantee())) {
            throw new OrderException("You can only end your own shares.");
        }
        shares.delete(s);
        audit.log("UNSHARE", "Records", s.isAllRecords() ? s.getOwner() + " (all)" : s.getOrderType() + " " + s.getOrderCode(), "for " + s.getGrantee());
    }

    /** Administrators: every record of one user becomes another user's (e.g. when someone leaves). */
    @Transactional
    public int handOver(AppUser by, String from, String to) {
        if (!by.isAdmin()) throw new OrderException("Only administrators can hand over records.");
        if (from == null || to == null || from.equals(to) || users.findByUsername(to).isEmpty()) {
            throw new OrderException("Choose another user to hand the records to.");
        }
        int n = lines.moveCreator(from, to);
        shares.moveOwner(from, to);
        audit.log("HAND_OVER", "Records", from, n + " order lines to " + to);
        return n;
    }
}
