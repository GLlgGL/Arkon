package com.fms.service;

import com.fms.config.FmsProperties;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Files on disk: invoice PDFs and the invoice logo.
 * Replaces ~/InvoicesPrint/{Purchase,Sales,Offers} Invoice/ and ~/Logo/ in the IIS site.
 *
 * The original passed full server paths through the page (CommandArgument) and
 * deleted whatever path came back. Here only a bare file name is accepted and
 * it must resolve inside the expected folder.
 */
@Service
public class StorageService {

    private static final Set<String> LOGO_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif");

    private final Path root;

    public StorageService(FmsProperties props) {
        this.root = Path.of(props.storageDir()).toAbsolutePath().normalize();
        try {
            for (OrderType t : OrderType.values()) {
                Files.createDirectories(invoiceDir(t));
            }
            Files.createDirectories(logoDir());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage folders under " + root, e);
        }
    }

    /** The storage folder (invoices, logo, backups). */
    public Path root() {
        return root;
    }

    public Path invoiceDir(OrderType type) {
        return root.resolve("invoices").resolve(type.getPath());
    }

    public Path logoDir() {
        return root.resolve("logo");
    }

    public record StoredFile(String name, long size, Instant modified) {
        /** Local date-time for display in templates. */
        public java.time.LocalDateTime modifiedAt() {
            return java.time.LocalDateTime.ofInstant(modified, java.time.ZoneId.systemDefault());
        }
    }

    public List<StoredFile> listInvoices(OrderType type, String search) {
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        try (Stream<Path> files = Files.list(invoiceDir(type))) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(needle))
                    .map(StorageService::describe)
                    .sorted(Comparator.comparing(StoredFile::modified).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path resolveInvoice(OrderType type, String fileName) {
        return safeResolve(invoiceDir(type), fileName);
    }

    public void deleteInvoice(OrderType type, String fileName) {
        try {
            Files.deleteIfExists(resolveInvoice(type, fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Saves the uploaded logo and returns its file name. Replaces any previous logo. */
    public String storeLogo(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!LOGO_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("The logo must be a PNG, JPG or GIF image.");
        }
        String name = "logo." + ext;
        try (InputStream in = file.getInputStream(); Stream<Path> old = Files.list(logoDir())) {
            old.forEach(p -> p.toFile().delete());
            Files.copy(in, logoDir().resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path logoPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        Path p = logoDir().resolve(fileName).normalize();
        return p.getParent().equals(logoDir()) && Files.isRegularFile(p) ? p : null;
    }

    public void deleteLogo(String fileName) {
        Path p = logoPath(fileName);
        if (p != null) {
            p.toFile().delete();
        }
    }

    /** Characters Windows and Linux both accept in a file name. */
    public static String safeFileName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
    }

    private static Path safeResolve(Path dir, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new NotFoundException("File not found");
        }
        Path p = dir.resolve(fileName).normalize();
        if (!dir.equals(p.getParent()) || !Files.isRegularFile(p)) {
            throw new NotFoundException("File not found: " + fileName);
        }
        return p;
    }

    private static StoredFile describe(Path p) {
        try {
            return new StoredFile(p.getFileName().toString(), Files.size(p), Files.getLastModifiedTime(p).toInstant());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
