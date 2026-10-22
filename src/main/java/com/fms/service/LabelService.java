package com.fms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Barcode labels: product codes drawn as sharp SVG barcodes (EAN-13 for 13-digit codes, otherwise Code 128). */
@Service("labels")
public class LabelService {

    /** Label sizes: key -> width x height in mm (A4 sheets: the label inside a 3 x 8 grid). */
    public record Size(String key, double width, double height, boolean sheet) { }

    public static final Map<String, Size> SIZES = Map.of(
            "50x30", new Size("50x30", 50, 30, false),
            "40x25", new Size("40x25", 40, 25, false),
            "58x40", new Size("58x40", 58, 40, false),
            "a4-3x8", new Size("a4-3x8", 70, 37.125, true));

    public static Size size(String key) { return SIZES.getOrDefault(key, SIZES.get("50x30")); }

    /** An SVG barcode for a code (null when the code can't be drawn). */
    public String svg(String code) {
        if (code == null || code.isBlank()) return null;
        String c = code.trim();
        try {
            BitMatrix m = isEan13(c)
                    ? new EAN13Writer().encode(c, BarcodeFormat.EAN_13, 0, 0)
                    : new Code128Writer().encode(c, BarcodeFormat.CODE_128, 0, 0, Map.of(EncodeHintType.MARGIN, 0));
            int w = m.getWidth();
            StringBuilder sb = new StringBuilder();
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(w).append(" 40\" preserveAspectRatio=\"none\" class=\"bc\" role=\"img\" aria-label=\"")
              .append(escape(c)).append("\"><path d=\"");
            int x = 0;
            while (x < w) {
                if (m.get(x, 0)) {
                    int start = x;
                    while (x < w && m.get(x, 0)) x++;
                    sb.append('M').append(start).append(" 0h").append(x - start).append("v40h-").append(x - start).append('z');
                } else x++;
            }
            return sb.append("\"/></svg>").toString();
        } catch (Exception e) {
            return null;            // e.g. characters a barcode can't hold
        }
    }

    /** 13 digits with a valid check digit. */
    static boolean isEan13(String c) {
        if (!c.matches("\\d{13}")) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (c.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return (10 - sum % 10) % 10 == c.charAt(12) - '0';
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
    }
}
