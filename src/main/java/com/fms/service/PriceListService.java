package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Price lists (e.g. Wholesale, Retail): a price per product. A client given a
 * list gets its prices on sales and offers; products not on the list keep the
 * normal sale price. Class discounts still apply on top.
 */
@Service("priceLists")
public class PriceListService {

    private final PriceListRepository lists;
    private final PriceListItemRepository items;
    private final ClientRepository clients;
    private final ProductRepository products;
    private final ConfigService config;
    private final AuditService audit;

    public PriceListService(PriceListRepository lists, PriceListItemRepository items, ClientRepository clients,
                            ProductRepository products, ConfigService config, AuditService audit) {
        this.lists = lists; this.items = items; this.clients = clients; this.products = products; this.config = config; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PriceList> all() { return lists.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public List<PriceList> active() { return lists.findByActiveTrueOrderByNameAsc(); }

    public PriceList get(Long id) { return lists.findById(id).orElseThrow(() -> new NotFoundException("Price list not found")); }

    /** productId -> list price */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> prices(Long listId) {
        Map<Long, BigDecimal> m = new HashMap<>();
        items.findByListId(listId).forEach(i -> m.put(i.getProductId(), i.getPrice()));
        return m;
    }

    @Transactional
    public PriceList save(Long id, String name, String description, boolean active) {
        if (name == null || name.isBlank()) throw new OrderException("Enter a name for the price list.");
        if (id == null ? lists.existsByName(name.trim()) : lists.existsByNameAndIdNot(name.trim(), id)) {
            throw new OrderException("A price list with this name already exists.");
        }
        PriceList l = id == null ? new PriceList() : get(id);
        l.setName(name.trim());
        l.setDescription(description == null || description.isBlank() ? null : description.trim());
        l.setActive(active);
        PriceList saved = lists.save(l);
        audit.log(id == null ? "CREATE" : "UPDATE", "Price list", saved.getName(), null);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        PriceList l = get(id);
        for (Client c : clients.findAll()) {
            if (id.equals(c.getPriceListId())) { c.setPriceListId(null); clients.save(c); }
        }
        lists.delete(l);
        audit.log("DELETE", "Price list", l.getName(), null);
    }

    /** Saves the prices; an empty price removes the product from the list. */
    @Transactional
    public void savePrices(Long listId, Map<Long, BigDecimal> prices) {
        get(listId);
        for (var e : prices.entrySet()) {
            var existing = items.findByListIdAndProductId(listId, e.getKey());
            if (e.getValue() == null || e.getValue().signum() < 0) {
                existing.ifPresent(x -> items.delete(x));
                continue;
            }
            PriceListItem i = existing.orElseGet(PriceListItem::new);
            i.setListId(listId);
            i.setProductId(e.getKey());
            i.setPrice(Money.round(e.getValue()));
            items.save(i);
        }
        audit.log("UPDATE", "Price list", get(listId).getName(), "prices saved");
    }

    /** Sets every product's list price to its sale price plus or minus a percentage. */
    @Transactional
    public void fillFromSalePrice(Long listId, BigDecimal percent) {
        BigDecimal f = BigDecimal.ONE.add(Money.nz(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        Map<Long, BigDecimal> m = new HashMap<>();
        for (Product p : products.findAll()) m.put(p.getId(), p.getSalePrice().multiply(f));
        savePrices(listId, m);
    }

    // ------------------------------------------------------------ clients

    @Transactional(readOnly = true)
    public Long listOfClient(Long clientId) {
        return clientId == null ? null : clients.findById(clientId).map(Client::getPriceListId).orElse(null);
    }

    @Transactional
    public void setClientList(Long clientId, Long listId) {
        Client c = clients.findById(clientId).orElseThrow(() -> new NotFoundException("Client not found"));
        c.setPriceListId(listId);
        clients.save(c);
        audit.log("UPDATE", "Client", c.getName(), "price list " + (listId == null ? "none" : get(listId).getName()));
    }

    /** The client's list price for a product, if the module is on and there is one. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> priceFor(Product p, String clientName) {
        if (!config.get().isModPriceLists() || clientName == null) return Optional.empty();
        Long listId = clients.findByName(clientName).map(Client::getPriceListId).orElse(null);
        if (listId == null || !lists.findById(listId).map(PriceList::isActive).orElse(false)) return Optional.empty();
        return items.findByListIdAndProductId(listId, p.getId()).map(PriceListItem::getPrice);
    }
}
