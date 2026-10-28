package com.fms.service;

import com.fms.domain.Asset;
import com.fms.domain.NotFoundException;
import com.fms.repo.AssetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixed assets (asete): register, depreciation and disposal. Depreciation is
 * monthly, starting the month after purchase:
 *  - STRAIGHT: (cost - salvage) / useful life in months, every month;
 *  - DECLINING: RATE% per year of the remaining book value (divided by 12 each month).
 * The book value never goes below the salvage value.
 */
@Service
public class AssetService {

    public record Month(YearMonth month, BigDecimal depreciation, BigDecimal accumulated, BigDecimal bookValue) {
    }

    public record AssetView(Asset asset, BigDecimal accumulated, BigDecimal bookValue, BigDecimal periodDepreciation,
                            BigDecimal gainLoss) {
    }

    private final AssetRepository repo;
    private final AuditService audit;

    public AssetService(AssetRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public Asset get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Asset not found"));
    }

    /** The whole depreciation schedule, month by month, until fully depreciated (or disposed). */
    public List<Month> schedule(Asset a) {
        List<Month> out = new ArrayList<>();
        BigDecimal cost = a.getCost();
        BigDecimal floor = Money.nz(a.getSalvage());
        BigDecimal book = cost;
        BigDecimal acc = BigDecimal.ZERO;
        YearMonth m = YearMonth.from(a.getPurchaseDate()).plusMonths(1);
        YearMonth last = a.isDisposed() && a.getDisposalDate() != null ? YearMonth.from(a.getDisposalDate()) : null;
        int life = a.getLifeMonths() == null || a.getLifeMonths() < 1 ? 1 : a.getLifeMonths();
        BigDecimal straight = cost.subtract(floor).divide(BigDecimal.valueOf(life), 2, RoundingMode.HALF_UP);
        for (int i = 0; i < 1200 && book.compareTo(floor) > 0; i++, m = m.plusMonths(1)) {
            if (last != null && m.isAfter(last)) {
                break;
            }
            BigDecimal d = "DECLINING".equals(a.getMethod())
                    ? book.multiply(Money.nz(a.getRate())).divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP)
                    : straight;
            if ("DECLINING".equals(a.getMethod()) && i == life - 1) {
                d = book.subtract(floor);           // end of useful life: write down to salvage
            }
            d = d.min(book.subtract(floor)).max(BigDecimal.ZERO);
            if (d.signum() == 0) {
                break;
            }
            book = book.subtract(d);
            acc = acc.add(d);
            out.add(new Month(m, d, acc, book));
            if ("STRAIGHT".equals(a.getMethod()) && i >= life - 1) {
                break;
            }
        }
        return out;
    }

    /** Book value at the end of a date's month. */
    public BigDecimal bookValue(Asset a, LocalDate date) {
        BigDecimal book = a.getCost();
        if (date.isBefore(a.getPurchaseDate())) {
            return BigDecimal.ZERO;
        }
        YearMonth until = YearMonth.from(date);
        for (Month m : schedule(a)) {
            if (m.month().isAfter(until)) {
                break;
            }
            book = m.bookValue();
        }
        return book;
    }

    /** Register at a date, with the depreciation of a period. */
    @Transactional(readOnly = true)
    public List<AssetView> register(LocalDate at, LocalDate periodFrom, LocalDate periodTo) {
        List<AssetView> out = new ArrayList<>();
        for (Asset a : repo.findAllByOrderByCodeAsc()) {
            BigDecimal book = bookValue(a, at);
            BigDecimal acc = at.isBefore(a.getPurchaseDate()) ? BigDecimal.ZERO : a.getCost().subtract(book);
            BigDecimal period = BigDecimal.ZERO;
            for (Month m : schedule(a)) {
                if (!m.month().isBefore(YearMonth.from(periodFrom)) && !m.month().isAfter(YearMonth.from(periodTo))) {
                    period = period.add(m.depreciation());
                }
            }
            BigDecimal gain = a.isDisposed() && a.getDisposalValue() != null && a.getDisposalDate() != null
                    ? a.getDisposalValue().subtract(bookValue(a, a.getDisposalDate())) : null;
            out.add(new AssetView(a, Money.round(acc), Money.round(book), Money.round(period), gain));
        }
        return out;
    }

    @Transactional
    public Asset save(Long id, String code, String name, String category, String location, LocalDate purchaseDate,
                      BigDecimal cost, BigDecimal salvage, Integer lifeMonths, String method, BigDecimal rate,
                      Long costCenterId, String note) {
        if (code == null || code.isBlank() || name == null || name.isBlank() || purchaseDate == null
                || cost == null || cost.signum() <= 0 || lifeMonths == null || lifeMonths < 1) {
            throw new OrderException("Enter code, name, purchase date, cost and useful life.");
        }
        String c = code.trim().toUpperCase();
        if (id == null ? repo.existsByCode(c) : repo.existsByCodeAndIdNot(c, id)) {
            throw new OrderException("An asset with this code already exists.");
        }
        if ("DECLINING".equals(method) && (rate == null || rate.signum() <= 0)) {
            throw new OrderException("Enter the yearly rate for declining depreciation.");
        }
        Asset a = id == null ? new Asset() : get(id);
        a.setCode(c);
        a.setName(name.trim());
        a.setCategory(category == null || category.isBlank() ? null : category.trim());
        a.setLocation(location == null || location.isBlank() ? null : location.trim());
        a.setPurchaseDate(purchaseDate);
        a.setCost(Money.round(cost));
        a.setSalvage(Money.round(Money.nz(salvage)));
        a.setLifeMonths(lifeMonths);
        a.setMethod("DECLINING".equals(method) ? "DECLINING" : "STRAIGHT");
        a.setRate(rate);
        a.setCostCenterId(costCenterId);
        a.setNote(note == null || note.isBlank() ? null : note.trim());
        Asset saved = repo.save(a);
        audit.log(id == null ? "CREATE" : "UPDATE", "Asset", saved.getCode(), saved.getName() + ", " + Money.format(saved.getCost()));
        return saved;
    }

    @Transactional
    public void dispose(Long id, LocalDate date, BigDecimal value) {
        Asset a = get(id);
        if (date == null) {
            throw new OrderException("Enter the disposal date.");
        }
        a.setStatus("DISPOSED");
        a.setDisposalDate(date);
        a.setDisposalValue(Money.round(Money.nz(value)));
        repo.save(a);
        audit.log("DISPOSE", "Asset", a.getCode(), "on " + date + " for " + Money.format(a.getDisposalValue()));
    }

    @Transactional
    public void undispose(Long id) {
        Asset a = get(id);
        a.setStatus("ACTIVE");
        a.setDisposalDate(null);
        a.setDisposalValue(null);
        repo.save(a);
        audit.log("UNDISPOSE", "Asset", a.getCode(), null);
    }

    @Transactional
    public void delete(Long id) {
        Asset a = get(id);
        repo.delete(a);
        audit.log("DELETE", "Asset", a.getCode(), a.getName());
    }

    // ------------------------------------------------------------------ reports

    /** A disposal: cost, depreciation up to it, book value, what it was sold for, gain (+) or loss (−). */
    public record Disposal(Asset asset, BigDecimal cost, BigDecimal accumulated, BigDecimal bookValue, BigDecimal value, BigDecimal gainLoss) { }
    /** Depreciation of one asset in each month of a year. */
    public record DepRow(Asset asset, BigDecimal[] months, BigDecimal total) { }
    /** The movement of one category over a period (the assets report). */
    public record Movement(String category, BigDecimal costStart, BigDecimal additions, BigDecimal disposals, BigDecimal costEnd,
                           BigDecimal accStart, BigDecimal depreciation, BigDecimal accDisposed, BigDecimal accEnd,
                           BigDecimal bookStart, BigDecimal bookEnd) { }

    private static boolean heldOn(Asset a, LocalDate d) {
        return !a.getPurchaseDate().isAfter(d) && !(a.isDisposed() && a.getDisposalDate() != null && !a.getDisposalDate().isAfter(d));
    }

    @Transactional(readOnly = true)
    public List<Asset> acquisitions(LocalDate from, LocalDate to) {
        return repo.findAllByOrderByCodeAsc().stream()
                .filter(a -> !a.getPurchaseDate().isBefore(from) && !a.getPurchaseDate().isAfter(to)).toList();
    }

    @Transactional(readOnly = true)
    public List<Disposal> disposals(LocalDate from, LocalDate to) {
        List<Disposal> out = new ArrayList<>();
        for (Asset a : repo.findAllByOrderByCodeAsc()) {
            if (!a.isDisposed() || a.getDisposalDate() == null || a.getDisposalDate().isBefore(from) || a.getDisposalDate().isAfter(to)) continue;
            BigDecimal book = bookValue(a, a.getDisposalDate()), value = Money.nz(a.getDisposalValue());
            out.add(new Disposal(a, a.getCost(), Money.round(a.getCost().subtract(book)), Money.round(book), value, Money.round(value.subtract(book))));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<DepRow> depreciation(int year) {
        List<DepRow> out = new ArrayList<>();
        for (Asset a : repo.findAllByOrderByCodeAsc()) {
            BigDecimal[] m = new BigDecimal[12];
            java.util.Arrays.fill(m, BigDecimal.ZERO);
            BigDecimal total = BigDecimal.ZERO;
            for (Month x : schedule(a)) {
                if (x.month().getYear() != year) continue;
                m[x.month().getMonthValue() - 1] = x.depreciation();
                total = total.add(x.depreciation());
            }
            if (total.signum() != 0) out.add(new DepRow(a, m, total));
        }
        return out;
    }

    /** The assets report: per category, cost and accumulated depreciation at the start, movements, and at the end. */
    @Transactional(readOnly = true)
    public List<Movement> movement(LocalDate from, LocalDate to) {
        java.util.Map<String, BigDecimal[]> by = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        LocalDate before = from.minusDays(1);
        for (Asset a : repo.findAllByOrderByCodeAsc()) {
            String cat = a.getCategory() == null || a.getCategory().isBlank() ? "–" : a.getCategory();
            boolean atStart = heldOn(a, before);
            boolean bought = !a.getPurchaseDate().isBefore(from) && !a.getPurchaseDate().isAfter(to);
            boolean gone = a.isDisposed() && a.getDisposalDate() != null && !a.getDisposalDate().isBefore(from) && !a.getDisposalDate().isAfter(to);
            if (!atStart && !bought) continue;
            BigDecimal[] v = by.computeIfAbsent(cat, k -> { BigDecimal[] z = new BigDecimal[7]; java.util.Arrays.fill(z, BigDecimal.ZERO); return z; });
            if (atStart) { v[0] = v[0].add(a.getCost()); v[3] = v[3].add(a.getCost().subtract(bookValue(a, before))); }
            if (bought) v[1] = v[1].add(a.getCost());
            for (Month x : schedule(a)) {
                if (!x.month().isBefore(YearMonth.from(from)) && !x.month().isAfter(YearMonth.from(to))) v[4] = v[4].add(x.depreciation());
            }
            if (gone) { v[2] = v[2].add(a.getCost()); v[5] = v[5].add(a.getCost().subtract(bookValue(a, a.getDisposalDate()))); }
        }
        List<Movement> out = new ArrayList<>();
        for (var e : by.entrySet()) {
            BigDecimal[] v = e.getValue();
            BigDecimal costEnd = v[0].add(v[1]).subtract(v[2]), accEnd = v[3].add(v[4]).subtract(v[5]);
            out.add(new Movement(e.getKey(), Money.round(v[0]), Money.round(v[1]), Money.round(v[2]), Money.round(costEnd),
                    Money.round(v[3]), Money.round(v[4]), Money.round(v[5]), Money.round(accEnd),
                    Money.round(v[0].subtract(v[3])), Money.round(costEnd.subtract(accEnd))));
        }
        return out;
    }
}
