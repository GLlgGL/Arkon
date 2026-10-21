package com.fms.service;

import com.fms.domain.ExchangeRate;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderType;
import com.fms.repo.ExchangeRateRepository;
import com.fms.repo.OrderInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Currencies: the base currency (Settings, e.g. ALL) plus EUR and USD.
 * Rates are entered per day; the latest rate on or before a date is used.
 * Every amount is also kept in the base currency.
 */
@Service("fx")
public class CurrencyService {

    public static final List<String> FOREIGN = List.of("EUR", "USD");

    /** An order's currency and rate (rate 1 and the base code when it's in the base currency). */
    public record Fx(String code, BigDecimal rate, boolean foreign) {
        public BigDecimal of(BigDecimal base) {
            return base == null ? null : foreign ? base.divide(rate, 2, RoundingMode.HALF_UP) : base;
        }
    }

    private final ExchangeRateRepository rates;
    private final OrderInfoRepository infos;
    private final ConfigService config;
    private final AuditService audit;

    public CurrencyService(ExchangeRateRepository rates, OrderInfoRepository infos, ConfigService config, AuditService audit) {
        this.rates = rates; this.infos = infos; this.config = config; this.audit = audit;
    }

    public boolean enabled() { return config.get().isModCurrencies(); }
    public String base() { return config.get().getCurrency(); }
    public List<String> foreign() { return FOREIGN; }

    /** Latest rate on or before a date, or null. */
    @Transactional(readOnly = true)
    public BigDecimal rateOn(String currency, LocalDate date) {
        if (currency == null || !FOREIGN.contains(currency)) return BigDecimal.ONE;
        return rates.findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, date == null ? LocalDate.now() : date)
                .map(ExchangeRate::getRate).orElse(null);
    }

    /** Rate that must exist (for converting money). */
    public BigDecimal requireRate(String currency, LocalDate date) {
        BigDecimal r = rateOn(currency, date);
        if (r == null) throw new OrderException("Enter the " + currency + " exchange rate first (Administration → Exchange rates).");
        return r;
    }

    public BigDecimal toBase(BigDecimal amount, String currency, LocalDate date) {
        if (amount == null || currency == null || !FOREIGN.contains(currency)) return amount;
        return Money.round(amount.multiply(requireRate(currency, date)));
    }

    public BigDecimal fromBase(BigDecimal amount, String currency, LocalDate date) {
        if (amount == null || currency == null || !FOREIGN.contains(currency)) return amount;
        BigDecimal r = rateOn(currency, date);
        return r == null ? amount : amount.divide(r, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Fx ofOrder(OrderType type, String code) {
        if (code != null && enabled()) {
            OrderInfo i = infos.findByOrderTypeAndOrderCode(type, code).orElse(null);
            if (i != null && i.isForeign()) return new Fx(i.getCurrency(), i.getFxRate(), true);
        }
        return new Fx(base(), BigDecimal.ONE, false);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> recent() { return rates.findTop60ByOrderByRateDateDescCurrencyAsc(); }

    @Transactional
    public void saveRate(LocalDate date, String currency, BigDecimal rate) {
        if (!FOREIGN.contains(currency)) throw new OrderException("Choose EUR or USD.");
        if (rate == null || rate.signum() <= 0) throw new OrderException("Enter a rate greater than 0.");
        LocalDate d = date == null ? LocalDate.now() : date;
        ExchangeRate r = rates.findByRateDateAndCurrency(d, currency).orElseGet(ExchangeRate::new);
        r.setRateDate(d);
        r.setCurrency(currency);
        r.setRate(rate.setScale(6, RoundingMode.HALF_UP));
        rates.save(r);
        audit.log("UPDATE", "Exchange rate", currency + " " + d, "1 " + currency + " = " + rate + " " + base());
    }

    @Transactional
    public void deleteRate(Long id) {
        rates.deleteById(id);
    }
}
