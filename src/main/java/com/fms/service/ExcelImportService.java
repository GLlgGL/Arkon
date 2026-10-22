package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Import from Excel: products, clients, merchants (suppliers) and opening stock.
 * Columns are found by their header (Albanian or English), so the templates and your own
 * files both work. Every row is checked first (preview); saving is all or nothing.
 */
@Service
public class ExcelImportService {

    public enum Kind { PRODUCTS, CLIENTS, MERCHANTS, OPENING_STOCK }

    /** One row of the file: what will happen to it. */
    public record RowResult(int row, String action, String name, String message) implements java.io.Serializable {
        public boolean error() { return "ERROR".equals(action); }
    }
    public record Preview(Kind kind, List<RowResult> rows, int create, int update, int skip, int errors) implements java.io.Serializable {
        public boolean ok() { return errors == 0 && create + update > 0; }
    }

    /** Columns per kind: key, header shown in the template, header words that are recognised. */
    private record Col(String key, String header, String... words) { }

    private static final Map<Kind, List<Col>> COLS = Map.of(
        Kind.PRODUCTS, List.of(
            new Col("name", "Emri / Name", "emri", "name", "produkti", "product", "pershkrimi"),
            new Col("code", "Kodi ose barkodi / Code or barcode", "kodi", "code", "barkod", "barcode"),
            new Col("unit", "Njesia / Unit", "njesia", "unit"),
            new Col("buy", "Cmimi i blerjes / Buy price", "blerj", "buy", "kosto", "cost"),
            new Col("sale", "Cmimi i shitjes / Sale price", "shitj", "sale", "price", "cmimi"),
            new Col("limit", "Stoku minimal / Quantity limit", "minimal", "limit", "minimum"),
            new Col("class", "Klasa / Class", "klasa", "class", "grup", "category", "kategori"),
            new Col("tracking", "Gjurmimi (NONE/BATCH/SERIAL) / Tracking", "gjurm", "tracking")),
        Kind.CLIENTS, partyCols(true),
        Kind.MERCHANTS, partyCols(false),
        Kind.OPENING_STOCK, List.of(
            new Col("product", "Produkti (emri ose kodi) / Product (name or code)", "produkt", "product", "emri", "name", "kodi", "code"),
            new Col("qty", "Sasia / Quantity", "sasia", "quantity", "qty"),
            new Col("cost", "Cmimi i blerjes pa TVSH / Unit cost without VAT", "cmim", "cost", "kosto", "price"),
            new Col("location", "Vendndodhja (kodi) / Location (code)", "vendndodh", "location", "dyqan", "shop"),
            new Col("batch", "Loti / Batch", "loti", "lot", "batch"),
            new Col("expiry", "Skadenca / Expiry date", "skad", "expiry"),
            new Col("serials", "Numrat serial / Serial numbers", "serial")));

    private static List<Col> partyCols(boolean client) {
        List<Col> c = new ArrayList<>(List.of(
            new Col("name", "Emri / Name", "emri", "name", "emertimi", "subjekti", "klienti", "client", "furnitori", "supplier", "merchant"),
            new Col("tin", "NIPT", "nipt", "nuis", "tin", "vat number"),
            new Col("address", "Adresa / Address", "adres", "address"),
            new Col("email", "Email", "email", "e-mail", "mail"),
            new Col("phone", "Telefoni / Phone", "telef", "phone", "cel", "mobile")));
        if (client) c.add(new Col("vat", "TVSH % e klientit (bosh = normale) / Client VAT %", "tvsh", "vat %", "vat"));
        else c.add(new Col("kind", "Lloji i blerjes (LOCAL/IMPORT/INVEST_LOCAL/INVEST_IMPORT/FARMER)", "lloji", "purchase type", "kind"));
        return c;
    }

    private final ProductRepository products;
    private final ProductClassRepository classes;
    private final ClientRepository clients;
    private final MerchantRepository merchants;
    private final LocationRepository locations;
    private final OrderService orders;
    private final PartyService parties;
    private final AuditService audit;

    public ExcelImportService(ProductRepository products, ProductClassRepository classes, ClientRepository clients,
                              MerchantRepository merchants, LocationRepository locations, OrderService orders,
                              PartyService parties, AuditService audit) {
        this.products = products; this.classes = classes; this.clients = clients; this.merchants = merchants;
        this.locations = locations; this.orders = orders; this.parties = parties; this.audit = audit;
    }

    // ------------------------------------------------------------------ templates

    public byte[] template(Kind kind) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(kind.name());
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            Row h = sh.createRow(0);
            List<Col> cols = COLS.get(kind);
            for (int i = 0; i < cols.size(); i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols.get(i).header());
                c.setCellStyle(head);
                sh.setColumnWidth(i, 26 * 256);
            }
            String[][] example = switch (kind) {
                case PRODUCTS -> new String[][] {{"Laptop Dell 15", "LP001", "cope", "65000", "82000", "2", "Kompjutera", "SERIAL"},
                                                 {"Kabell HDMI 2m", "5901234123457", "cope", "300", "700", "10", "Kabllo", "NONE"}};
                case CLIENTS -> new String[][] {{"Alba shpk", "K12345678A", "Rr. Myslym Shyri, Tirane", "info@alba.al", "0691234567", ""}};
                case MERCHANTS -> new String[][] {{"Distributori ABC", "L98765432B", "Durres", "shitje@abc.al", "0681112223", "LOCAL"}};
                case OPENING_STOCK -> new String[][] {{"Laptop Dell 15", "3", "65000", "", "", "", "SN1001, SN1002, SN1003"},
                                                      {"Kabell HDMI 2m", "40", "300", "", "", "", ""}};
            };
            for (int r = 0; r < example.length; r++) {
                Row row = sh.createRow(r + 1);
                for (int i = 0; i < example[r].length; i++) {
                    if (example[r][i].isEmpty()) continue;
                    if (example[r][i].matches("\\d+") && !cols.get(i).key().matches("code|tin|phone|serials|product|name"))
                        row.createCell(i).setCellValue(Double.parseDouble(example[r][i]));
                    else row.createCell(i).setCellValue(example[r][i]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new OrderException("The template could not be created.");
        }
    }

    // ------------------------------------------------------------------ reading

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** Rows of the first sheet as key -> text, keyed by the recognised header. Row numbers as in Excel. */
    private static List<Map.Entry<Integer, Map<String, String>>> read(Kind kind, byte[] file) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            Sheet sh = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(Locale.ROOT);
            Row header = sh.getRow(sh.getFirstRowNum());
            if (header == null) throw new OrderException("The file is empty.");
            Map<Integer, String> keyOfCol = new HashMap<>();
            for (Cell c : header) {
                String h = norm(fmt.formatCellValue(c));
                if (h.isEmpty()) continue;
                for (Col col : COLS.get(kind)) {
                    if (keyOfCol.containsValue(col.key())) continue;
                    if (Arrays.stream(col.words()).anyMatch(w -> h.contains(norm(w)))) { keyOfCol.put(c.getColumnIndex(), col.key()); break; }
                }
            }
            String first = COLS.get(kind).get(0).key();
            if (!keyOfCol.containsValue(first)) throw new OrderException("The first row must be the column headers, as in the template.");
            List<Map.Entry<Integer, Map<String, String>>> out = new ArrayList<>();
            for (int r = sh.getFirstRowNum() + 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;
                Map<String, String> m = new HashMap<>();
                for (var e : keyOfCol.entrySet()) {
                    Cell c = row.getCell(e.getKey());
                    if (c == null) continue;
                    String v;
                    if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                        v = c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                    } else if (c.getCellType() == CellType.NUMERIC) {
                        v = BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString();
                    } else {
                        v = fmt.formatCellValue(c);
                    }
                    if (v != null && !v.isBlank()) m.put(e.getValue(), v.trim());
                }
                if (!m.isEmpty()) out.add(Map.entry(r + 1, m));
            }
            return out;
        } catch (IOException | org.apache.poi.EmptyFileException e) {
            throw new OrderException("This isn't an Excel file Arkon can read (.xlsx or .xls).");
        }
    }

    private static BigDecimal num(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.replace(" ", "").replace("\u00a0", "");
        if (t.matches("-?\\d{1,3}(\\.\\d{3})+(,\\d+)?")) t = t.replace(".", "").replace(',', '.');     // 1.234,56
        else t = t.replace(",", t.contains(".") ? "" : ".");                                          // 1,234.56 or 12,5
        try { return new BigDecimal(t); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception ignored) { }
        try { return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")); } catch (Exception ignored) { }
        try { return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy")); } catch (Exception ignored) { }
        return null;
    }

    // ------------------------------------------------------------------ check and save

    @Transactional(readOnly = true)
    public Preview preview(Kind kind, byte[] file, boolean update) {
        return process(kind, file, update, false, null);
    }

    /** Saves everything, or nothing if any row has an error. */
    @Transactional
    public Preview apply(Kind kind, byte[] file, boolean update, AppUser user) {
        Preview p = process(kind, file, update, false, null);
        if (!p.ok()) throw new OrderException("Fix the rows with errors first; nothing was saved.");
        Preview done = process(kind, file, update, true, user);
        audit.log("IMPORT", "Excel", kind.name(), done.create() + " created, " + done.update() + " updated, " + done.skip() + " unchanged");
        return done;
    }

    private Preview process(Kind kind, byte[] file, boolean update, boolean save, AppUser user) {
        List<RowResult> res = new ArrayList<>();
        var rows = read(kind, file);
        if (rows.isEmpty()) throw new OrderException("The file has no rows under the headers.");
        Set<String> seen = new HashSet<>();
        if (kind == Kind.OPENING_STOCK) return openingStock(rows, save, user);
        for (var e : rows) {
            int n = e.getKey();
            Map<String, String> v = e.getValue();
            String name = v.getOrDefault("name", "").trim();
            if (name.isEmpty()) { res.add(new RowResult(n, "ERROR", "", "The name is missing.")); continue; }
            if (!seen.add(norm(name))) { res.add(new RowResult(n, "ERROR", name, "This name is twice in the file.")); continue; }
            try {
                String action = switch (kind) {
                    case PRODUCTS -> product(v, name, update, save);
                    case CLIENTS -> party(v, name, update, save, true);
                    case MERCHANTS -> party(v, name, update, save, false);
                    default -> "SKIP";
                };
                res.add(new RowResult(n, action, name, ""));
            } catch (OrderException ex) {
                res.add(new RowResult(n, "ERROR", name, ex.getMessage()));
            }
        }
        return summary(kind, res);
    }

    private static Preview summary(Kind kind, List<RowResult> res) {
        int c = 0, u = 0, s = 0, e = 0;
        for (RowResult r : res) switch (r.action()) { case "CREATE" -> c++; case "UPDATE" -> u++; case "SKIP" -> s++; default -> e++; }
        return new Preview(kind, res, c, u, s, e);
    }

    private String product(Map<String, String> v, String name, boolean update, boolean save) {
        String code = v.get("code");
        Product p = products.findByName(name).orElse(null);
        if (p == null && code != null) p = products.findByCode(code).orElse(null);
        BigDecimal buy = num(v.get("buy")), sale = num(v.get("sale")), limit = num(v.get("limit"));
        if (v.containsKey("buy") && buy == null) throw new OrderException("The buy price isn't a number.");
        if (v.containsKey("sale") && sale == null) throw new OrderException("The sale price isn't a number.");
        if ((buy != null && buy.signum() < 0) || (sale != null && sale.signum() < 0)) throw new OrderException("Prices can't be negative.");
        if (p == null && sale == null) throw new OrderException("The sale price is missing.");
        if (code != null) {
            var other = products.findByCode(code).orElse(null);
            if (other != null && (p == null || !other.getId().equals(p.getId()))) throw new OrderException("The code " + code + " belongs to another product.");
        }
        String tracking = v.get("tracking") == null ? null : v.get("tracking").toUpperCase(Locale.ROOT);
        if (tracking != null && !List.of("NONE", "BATCH", "SERIAL").contains(tracking)) throw new OrderException("Tracking must be NONE, BATCH or SERIAL.");
        if (p != null && !update) return "SKIP";
        String action = p == null ? "CREATE" : "UPDATE";
        if (!save) return action;
        if (p == null) { p = new Product(); p.setName(name); p.setUnit("cope"); p.setBuyPrice(BigDecimal.ZERO); p.setQuantityLimit(0); }
        if (v.get("unit") != null) p.setUnit(v.get("unit"));
        if (buy != null) p.setBuyPrice(buy.setScale(2, RoundingMode.HALF_UP));
        if (sale != null) p.setSalePrice(sale.setScale(2, RoundingMode.HALF_UP));
        if (limit != null) p.setQuantityLimit(limit.intValue());
        if (code != null) p.setCode(code);
        if (tracking != null) p.setTracking(tracking);
        if (v.get("class") != null) p.setProductClass(classNamed(v.get("class")));
        products.save(p);
        return action;
    }

    private ProductClass classNamed(String name) {
        for (ProductClass c : classes.findAllByOrderByNameAsc()) if (c.getName().equalsIgnoreCase(name.trim())) return c;
        ProductClass c = new ProductClass();
        c.setName(name.trim());
        return classes.save(c);
    }

    private String party(Map<String, String> v, String name, boolean update, boolean save, boolean client) {
        String tin = v.get("tin");
        Party p = client ? clients.findByName(name).orElse(null) : merchants.findByName(name).orElse(null);
        if (p == null && tin != null) {
            for (Party x : client ? clients.findAll() : merchants.findAll()) if (tin.equalsIgnoreCase(x.getTin() == null ? "" : x.getTin())) { p = x; break; }
        }
        String email = v.get("email");
        if (email != null && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new OrderException("The email isn't valid.");
        BigDecimal vat = client ? num(v.get("vat")) : null;
        if (client && v.containsKey("vat") && (vat == null || vat.signum() < 0 || vat.compareTo(BigDecimal.valueOf(100)) > 0)) throw new OrderException("The VAT % must be between 0 and 100.");
        String kind = client ? null : v.get("kind") == null ? null : v.get("kind").toUpperCase(Locale.ROOT);
        if (kind != null && !List.of("LOCAL", "IMPORT", "INVEST_LOCAL", "INVEST_IMPORT", "FARMER").contains(kind)) throw new OrderException("Unknown purchase type: " + kind + ".");
        if (p != null && !update) return "SKIP";
        String action = p == null ? "CREATE" : "UPDATE";
        if (!save) return action;
        if (p == null) { p = client ? new Client() : new Merchant(); p.setName(name); }
        if (tin != null) p.setTin(tin);
        if (v.get("address") != null) p.setAddress(v.get("address"));
        if (email != null) p.setEmail(email);
        if (v.get("phone") != null) p.setPhone(v.get("phone"));
        if (client && vat != null) ((Client) p).setVatRate(vat);
        if (!client && kind != null) ((Merchant) p).setPurchaseKind(kind);
        if (client) clients.save((Client) p); else merchants.save((Merchant) p);
        return action;
    }

    /** Opening stock: one opening purchase per location, from the merchant "Gjendja fillestare". */
    private Preview openingStock(List<Map.Entry<Integer, Map<String, String>>> rows, boolean save, AppUser user) {
        List<RowResult> res = new ArrayList<>();
        Map<Long, List<Object[]>> byLocation = new LinkedHashMap<>();   // location id (or -1) -> [product, qty, cost, batch, expiry, serials, row]
        for (var e : rows) {
            int n = e.getKey();
            Map<String, String> v = e.getValue();
            String ref = v.getOrDefault("product", "");
            Product p = products.findByName(ref).orElseGet(() -> products.findByCode(ref).orElse(null));
            if (p == null) { res.add(new RowResult(n, "ERROR", ref, "No product with this name or code. Import the products first.")); continue; }
            BigDecimal qty = num(v.get("qty")), cost = num(v.get("cost"));
            if (qty == null || qty.signum() <= 0 || qty.stripTrailingZeros().scale() > 0) { res.add(new RowResult(n, "ERROR", p.getName(), "The quantity must be a whole number above 0.")); continue; }
            if (v.containsKey("cost") && (cost == null || cost.signum() < 0)) { res.add(new RowResult(n, "ERROR", p.getName(), "The unit cost isn't a valid number.")); continue; }
            Long loc = -1L;
            if (v.get("location") != null) {
                var l = locations.findAll().stream().filter(x -> x.getCode().equalsIgnoreCase(v.get("location")) || x.getName().equalsIgnoreCase(v.get("location"))).findFirst();
                if (l.isEmpty()) { res.add(new RowResult(n, "ERROR", p.getName(), "Unknown location: " + v.get("location") + ".")); continue; }
                loc = l.get().getId();
            }
            if (p.isBatchTracked() && v.get("batch") == null) { res.add(new RowResult(n, "ERROR", p.getName(), "This product is batch-tracked: fill in the batch.")); continue; }
            if (p.isSerialTracked() && TrackingService.parse(v.get("serials")).size() != qty.intValue()) {
                res.add(new RowResult(n, "ERROR", p.getName(), "This product has serial numbers: enter one per unit (" + qty.intValue() + ").")); continue;
            }
            byLocation.computeIfAbsent(loc, k -> new ArrayList<>()).add(new Object[] {p, qty.intValue(), cost, v.get("batch"), date(v.get("expiry")), v.get("serials"), n});
            res.add(new RowResult(n, "CREATE", p.getName(), qty.toPlainString() + " × " + (cost == null ? p.getBuyPrice() : cost)));
        }
        Preview prev = summary(Kind.OPENING_STOCK, res);
        if (!save || prev.errors() > 0) return prev;
        String merchant = "Gjendja fillestare";
        if (merchants.findByName(merchant).isEmpty()) { Merchant m = new Merchant(); m.setName(merchant); merchants.save(m); }
        List<String> companies = parties.providerNames(OrderType.SALE);
        if (companies.isEmpty()) throw new OrderException("Add your company first (Administration → Companies).");
        for (var g : byLocation.entrySet()) {
            String code = null;
            for (Object[] l : g.getValue()) {
                Product p = (Product) l[0];
                AddLineForm f = new AddLineForm();
                f.setProvider(merchant);
                f.setBuyer(companies.get(0));
                f.setProduct(p.getName());
                f.setQuantity((Integer) l[1]);
                f.setUnitPrice(l[2] == null ? p.getBuyPrice() : (BigDecimal) l[2]);
                f.setBatchNo((String) l[3]);
                f.setExpiryDate((LocalDate) l[4]);
                f.setSerials((String) l[5]);
                if (g.getKey() > 0) f.setLocationId(g.getKey());
                code = orders.addLine(OrderType.PURCHASE, code, f, user).getOrderCode();
            }
        }
        return prev;
    }
}
