package com.fms.service;

import com.fms.domain.AppConfig;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderType;
import com.fms.domain.Product;
import com.fms.repo.ClientClassDiscountRepository;
import com.fms.repo.ClientRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.ProductRepository;
import com.fms.repo.StockAdjustmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The pricing rules from ProductInfo(), Discount(), ProductAvailable() and
 * Amount_TextChanged() in the NewSale / NewOffer / AddNewPruchase pages.
 *
 * Purchase: unit price = product buy price, no discount, no stock limit.
 * Sale:     unit price = sale price less the client's discount for the
 *           product's class; more than the stock on hand is refused.
 * Offer:    as a sale; for a non-client the discount is typed in by hand.
 * All:      VAT = value without VAT x VAT rate from settings.
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProductRepository products;
    private final ClientRepository clients;
    private final ClientClassDiscountRepository discounts;
    private final OrderLineRepository lines;
    private final ConfigService configService;
    private final StockAdjustmentRepository adjustments;

    public PricingService(ProductRepository products, ClientRepository clients,
                          ClientClassDiscountRepository discounts, OrderLineRepository lines,
                          ConfigService configService, StockAdjustmentRepository adjustments) {
        this.products = products;
        this.clients = clients;
        this.discounts = discounts;
        this.lines = lines;
        this.configService = configService;
        this.adjustments = adjustments;
    }

    /**
     * Units in stock: everything purchased, minus everything sold, plus stock
     * adjustments (returns, damage, stock counts). Offers don't count.
     */
    @Transactional(readOnly = true)
    public long available(String productName) {
        Long loc = StockLocation.get();
        if (loc != null && locationRepo != null && configService.get().isModLocations()) {
            return availableAt(productName, loc);
        }
        return lines.sumQuantity(OrderType.PURCHASE, productName).longValue()
                - lines.sumQuantity(OrderType.SALE, productName).longValue()
                + adjustments.sumQuantity(productName).longValue();
    }

    /** Stock at a location when Locations is on (null = the main location); otherwise the total stock. */
    public long availableFor(String productName, Long loc) {
        if (locationRepo == null || !configService.get().isModLocations()) return available(productName);
        Long id = loc != null ? loc : locationRepo.findFirstByMainTrue().map(com.fms.domain.Location::getId).orElse(null);
        return id == null ? available(productName) : availableAt(productName, id);
    }

    /** Stock of a product at one location. */
    public long availableAt(String productName, Long loc) {
        boolean main = locationRepo.findById(loc).map(com.fms.domain.Location::isMain).orElse(false);
        return lines.sumQuantityAt(OrderType.PURCHASE, productName, loc, main).longValue()
                - lines.sumQuantityAt(OrderType.SALE, productName, loc, main).longValue()
                + adjustments.sumQuantityAt(productName, loc, main).longValue();
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.fms.repo.LocationRepository locationRepo;

    /**
     * Average purchase cost per unit (weighted by quantity) over all purchases
     * of the product. Falls back to the product's buy price when it was never
     * purchased through the app.
     */
    @Transactional(readOnly = true)
    public BigDecimal averageCost(Product product) {
        long qty = lines.sumQuantity(OrderType.PURCHASE, product.getName()).longValue();
        BigDecimal value = new BigDecimal(lines.sumValueNoVat(OrderType.PURCHASE, product.getName()).toString());
        // Produced goods count at their production cost (Production module).
        if (productionOrders != null) {
            for (Object[] r : productionOrders.producedTotals(product.getName())) {
                qty += ((Number) r[0]).longValue();
                value = value.add(new BigDecimal(r[1].toString()));
            }
        }
        if (qty <= 0) {
            return Money.round(product.getBuyPrice());
        }
        return value.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
    }

    private com.fms.repo.ProductionOrderRepository productionOrders;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setProductionOrders(com.fms.repo.ProductionOrderRepository r) {
        this.productionOrders = r;
    }

    /**
     * @param buyerName      client name (sales/offers); may be a non-client for offers
     * @param manualDiscount discount typed in for an offer to a non-client; ignored otherwise
     */
    @Transactional(readOnly = true)
    public Quote quote(OrderType type, String productName, String buyerName, BigDecimal manualDiscount, int requestedQty) {
        return quote(type, productName, buyerName, manualDiscount, requestedQty, null);
    }

    /**
     * @param priceOverride purchases only: the unit price actually paid, when it
     *                      differs from the product's buy price; null to use the buy price
     */
    @Transactional(readOnly = true)
    public Quote quote(OrderType type, String productName, String buyerName, BigDecimal manualDiscount,
                       int requestedQty, BigDecimal priceOverride) {
        Product product = products.findByName(productName)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productName));
        AppConfig config = configService.get();
        int requested = Math.max(requestedQty, 1);

        BigDecimal base;
        BigDecimal discount = BigDecimal.ZERO;
        Long available = null;
        int qty = requested;
        boolean exceedsStock = false;

        if (type == OrderType.PURCHASE) {
            base = priceOverride != null && priceOverride.signum() >= 0 ? priceOverride : product.getBuyPrice();
        } else {
            base = product.getSalePrice();
            if (priceLists != null && type != OrderType.PURCHASE) {
                base = priceLists.priceFor(product, buyerName).orElse(base);     // the client's price list
            }
            discount = discountFor(type, product, buyerName, manualDiscount);
            available = available(productName);
            exceedsStock = requested > available;
        }

        BigDecimal unitPrice = Money.round(base.multiply(HUNDRED.subtract(discount)).divide(HUNDRED, 6, RoundingMode.HALF_UP));
        BigDecimal noVat = Money.round(unitPrice.multiply(BigDecimal.valueOf(qty)));
        BigDecimal vatRate = config.getVatRate();
        if (type != OrderType.PURCHASE && buyerName != null) {        // the client's own VAT rate, if set
            var c = clients.findByName(buyerName).orElse(null);
            if (c != null && c.getVatRate() != null) {
                vatRate = c.getVatRate().divide(HUNDRED, 6, RoundingMode.HALF_UP);
            }
        }
        BigDecimal vat = Money.round(noVat.multiply(vatRate));
        BigDecimal withVat = noVat.add(vat);

        // The quantity limit is the minimum stock to keep: warn when a sale or
        // offer would leave less than that in stock. (The old sale page warned
        // when one sale was larger than the limit.)
        Long remainingAfter = available == null ? null : available - qty;
        boolean overLimit = type.isOutgoing() && product.getQuantityLimit() > 0
                && !exceedsStock && remainingAfter < product.getQuantityLimit();

        return new Quote(product.getName(), product.getUnit(), Money.round(base), discount, unitPrice,
                requested, qty, available, remainingAfter, product.getQuantityLimit(), exceedsStock, overLimit,
                noVat, vat, withVat);
    }

    private BigDecimal discountFor(OrderType type, Product product, String buyerName, BigDecimal manualDiscount) {
        var client = buyerName == null ? null : clients.findByName(buyerName).orElse(null);
        if (client == null) {
            if (type == OrderType.OFFER && manualDiscount != null) {
                return manualDiscount.max(BigDecimal.ZERO).min(HUNDRED);
            }
            return BigDecimal.ZERO;
        }
        if (product.getProductClass() == null) {
            return BigDecimal.ZERO;
        }
        // Note: the original looped over every PClasses/CClass row and kept only
        // the result of the last row, so most discounts silently came out as 0.
        // This looks up the one row that applies.
        return discounts.findByClientIdAndProductClassId(client.getId(), product.getProductClass().getId())
                .map(d -> d.getDiscountPercent())
                .orElse(BigDecimal.ZERO);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private PriceListService priceLists;
}
