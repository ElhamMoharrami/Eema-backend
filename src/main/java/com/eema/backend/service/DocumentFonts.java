package com.eema.backend.service;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.*;
import java.io.*;
import java.util.*;

/** Application-controlled bytes only; PDFont instances belong exclusively to one document. */
final class DocumentFonts {
    private static final List<String> RESOURCES = List.of("NotoSans-Regular.ttf", "NotoSans-Bold.ttf",
            "EemaDocumentCJK-Regular.ttf", "NotoNaskhArabic-Regular.ttf");
    private static final List<byte[]> BYTES = readResources();
    private final PDDocument document;
    private final Map<Integer, PDType0Font> loaded = new HashMap<>();
    DocumentFonts(PDDocument document) { this.document = document; }

    private static List<byte[]> readResources() {
        List<byte[]> result = new ArrayList<>();
        for (String name : RESOURCES) {
            try (InputStream in = DocumentFonts.class.getResourceAsStream("/fonts/" + name)) {
                if (in == null) throw new IOException("Missing required bundled font " + name);
                result.add(in.readAllBytes());
            } catch (IOException e) { throw new IllegalStateException("Required document font unavailable: " + name, e); }
        }
        return List.copyOf(result);
    }
    private static boolean validated;
    static synchronized void validateResources() {
        if (validated) return;
        try (PDDocument doc = new PDDocument()) {
            DocumentFonts fonts = new DocumentFonts(doc);
            for (int i = 0; i < RESOURCES.size(); i++) fonts.load(i).getStringWidth(i == 3 ? "م" : "Eema");
            validated = true;
        } catch (IOException | RuntimeException e) { throw new IllegalStateException("Required bundled document fonts are missing or corrupt", e); }
    }
    private PDType0Font load(int index) throws IOException {
        PDType0Font font = loaded.get(index);
        if (font == null) {
            font = PDType0Font.load(document, new ByteArrayInputStream(BYTES.get(index)), true);
            // ICU performs Arabic shaping. Disable PDFBox's partial GSUB to keep measured and shown text identical.
            ((PDCIDFontType2)font.getDescendantFont()).getTrueTypeFont().setEnableGsub(false);
            loaded.put(index, font);
        }
        return font;
    }
    PDType0Font font(int cp, boolean bold) throws IOException {
        int[] order = Character.UnicodeScript.of(cp) == Character.UnicodeScript.ARABIC
                ? new int[]{3, bold ? 1 : 0, 2} : new int[]{bold ? 1 : 0, 2, 3};
        for (int index : order) {
            PDType0Font f = load(index);
            if (((PDCIDFontType2)f.getDescendantFont()).getTrueTypeFont()
                    .getUnicodeCmapLookup().getGlyphId(cp) != 0) return f;
        }
        throw new IllegalStateException("Document generation cannot render unsupported code point U+" + Integer.toHexString(cp).toUpperCase(Locale.ROOT));
    }
    String visual(String value) throws IOException {
        String original = value == null || value.isEmpty() ? "-" : value;
        for (int cp : original.codePoints().toArray()) {
            int type = Character.getType(cp);
            // The released PDFBox has no GPOS. Do not pretend that marks or join controls are positioned correctly.
            if (Character.isISOControl(cp) || type == Character.FORMAT || type == Character.NON_SPACING_MARK
                    || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK)
                throw new IllegalStateException("Document generation cannot safely lay out code point U+" + Integer.toHexString(cp).toUpperCase(Locale.ROOT));
            font(cp, false); // Validate originals as well as shaped glyphs, before any private object is stored.
        }
        try {
            String shaped = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_LOGICAL).shape(original);
            Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            return bidi.writeReordered(Bidi.DO_MIRRORING);
        } catch (ArabicShapingException e) { throw new IllegalStateException("Document generation could not shape supported text", e); }
    }
    float width(String value, boolean bold, float size) throws IOException {
        float width = 0;
        for (int cp : visual(value).codePoints().toArray()) width += font(cp, bold).getStringWidth(Character.toString(cp)) * size / 1000;
        return width;
    }
}
