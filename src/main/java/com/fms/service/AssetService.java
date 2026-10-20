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
}
