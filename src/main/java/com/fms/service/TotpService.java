package com.fms.service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Two-step login codes (TOTP, RFC 6238): the 6-digit codes of authenticator
 * apps such as Google or Microsoft Authenticator. 30-second steps; a code
 * one step before or after is accepted too, for clocks that are slightly off.
 */
@Service
public class TotpService {

    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final SecureRandom random = new SecureRandom();

    public String newSecret() {
        byte[] b = new byte[20];
        random.nextBytes(b);
        return base32(b);
    }

    public boolean verify(String secret, String code) {
        if (secret == null || code == null) return false;
        String c = code.replaceAll("\\s", "");
        if (!c.matches("\\d{6}")) return false;
        long step = System.currentTimeMillis() / 1000 / 30;
        for (long s = step - 1; s <= step + 1; s++) {
            if (code(secret, s).equals(c)) return true;
        }
        return false;
    }

    String code(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(unbase32(secret), "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int o = h[h.length - 1] & 0x0f;
            int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16) | ((h[o + 2] & 0xff) << 8) | (h[o + 3] & 0xff);
            return String.format("%06d", bin % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The link authenticator apps read from the QR code. */
    public String uri(String username, String secret) {
        String label = URLEncoder.encode("Arkon:" + username, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=Arkon&digits=6&period=30";
    }

    /** QR code of the link, as a PNG data URI for an <img>. */
    public String qr(String uri) {
        try {
            var matrix = new com.google.zxing.qrcode.QRCodeWriter().encode(uri, com.google.zxing.BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** Secret shown in groups of four for typing in by hand. */
    public static String grouped(String secret) {
        return secret == null ? "" : secret.replaceAll("(.{4})", "$1 ").trim();
    }

    static String base32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(B32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) sb.append(B32.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    static byte[] unbase32(String s) {
        String c = s.replaceAll("[\\s=]", "").toUpperCase();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char ch : c.toCharArray()) {
            int v = B32.indexOf(ch);
            if (v < 0) continue;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
