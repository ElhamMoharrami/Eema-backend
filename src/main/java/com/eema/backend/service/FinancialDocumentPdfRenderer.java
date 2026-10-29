package com.eema.backend.service;

import com.eema.backend.entity.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class FinancialDocumentPdfRenderer {
    private static final String DISCLAIMER = "This document is a payment record and is not presented as a tax invoice.";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm XXX", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    public FinancialDocumentPdfRenderer() { DocumentFonts.validateResources(); }

    public byte[] bookingReceipt(String number, Booking b, Venue venue) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Reference", number)); rows.add(new Row("Issued for booking", b.getId().toString()));
        rows.add(new Row("Payment date", DATE.format(b.getPaidAt()))); rows.add(new Row("Booking status at issuance", b.getStatus().name()));
        rows.add(Row.section("Venue and shoot")); rows.add(new Row("Venue", venue.getName()));
        rows.add(new Row("Neighbourhood", venue.getNeighbourhood())); rows.add(new Row("Shoot date", DAY.format(b.getShootDate())));
        rows.add(new Row("Time", TIME.format(b.getStartTime()) + " - " + TIME.format(b.getEndTime())));
        rows.add(new Row("Venue timezone", venue.getTimezone())); rows.add(Row.section("Scout"));
        rows.add(new Row("Name", b.getScoutContact().getName())); rows.add(new Row("Email", b.getScoutContact().getEmail()));
        rows.add(Row.section("Pricing")); rows.add(new Row("Hourly rate snapshot", eur(b.getHourlyRateEurSnapshot())));
        rows.add(new Row("Minimum booking duration", minutes(b.getMinimumBookingMinutesSnapshot())));
        rows.add(new Row("Selected duration", minutes(b.getDurationMinutes()))); rows.add(new Row("Billable duration", minutes(b.getBillableMinutes())));
        if (b.getFullDayRateEurSnapshot() != null) rows.add(new Row("Full-day rate snapshot", eur(b.getFullDayRateEurSnapshot())));
        rows.add(new Row("Authoritative booking price", eur(b.getVenuePriceEur()))); rows.add(Row.section("Payment"));
        rows.add(new Row("Total paid", eur(b.getTotalPaidEur())));
        return render("Booking receipt", number, rows);
    }

    public byte[] payoutStatement(String number, OwnerPayout p, Booking b, Venue venue, User owner) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Reference", number)); rows.add(new Row("Booking reference", b.getId().toString()));
        rows.add(new Row("Payout reference", p.getId().toString()));
        rows.add(new Row("External transfer reference", p.getExternalTransferReference()));
        if (p.getPayoutDestination() != null)
            rows.add(new Row("Payout destination", "••••••••••" + p.getPayoutDestination().getIbanLast4()));
        rows.add(new Row("Paid at", DATE.format(p.getPaidAt()))); rows.add(Row.section("Owner and venue"));
        rows.add(new Row("Owner", owner.getFullName())); rows.add(new Row("Owner email", owner.getEmail()));
        rows.add(new Row("Venue", venue.getName())); rows.add(new Row("Shoot date", DAY.format(b.getShootDate())));
        rows.add(new Row("Shoot time", TIME.format(b.getStartTime()) + " - " + TIME.format(b.getEndTime()) + " (" + venue.getTimezone() + ")"));
        rows.add(Row.section("Payout breakdown")); rows.add(new Row("Gross retained booking amount", eur(p.getGrossRetainedAmountEur())));
        rows.add(new Row("Eema commission", eur(p.getCommissionEur()))); rows.add(new Row("Provider cost", eur(p.getProviderCostEur())));
        rows.add(new Row("Owner payout amount", eur(p.getPayoutAmountEur())));
        return render("Owner payout statement", number, rows);
    }

    private byte[] render(String title, String number, List<Row> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (PageWriter page = new PageWriter(document, new DocumentFonts(document), title, number)) {
                for (Row row : rows) page.row(row);
            }
            document.getDocumentInformation().setTitle(title);
            document.getDocumentInformation().setSubject("Eema payment record");
            document.save(out); return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Could not render financial document", e); }
    }

    private final class PageWriter implements AutoCloseable {
        private final PDDocument document;
        private final DocumentFonts fonts;
        private final String title, number;
        private PDPageContentStream content;
        private float y;

        PageWriter(PDDocument document, DocumentFonts fonts, String title, String number) throws IOException {
            this.document = document; this.fonts = fonts;
            this.title = title; this.number = number; nextPage();
        }
        private void nextPage() throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = 790;
            text(content, fonts, true, 22, 52, y, "Eema"); y -= 34;
            text(content, fonts, true, 17, 52, y, title); y -= 20;
            text(content, fonts, false, 9, 52, y, "Document reference: " + number); y -= 28;
        }
        void row(Row row) throws IOException {
            if (row.section) {
                if (y < 118) nextPage();
                y -= 8; text(content, fonts, true, 12, 52, y, row.label); y -= 18; return;
            }
            List<String> valueLines = lines(row.value, fonts, 9, PDRectangle.A4.getWidth() - 52 - 235);
            for (int i = 0; i < valueLines.size(); i++) {
                boolean continued = y < 80;
                if (continued) nextPage();
                if (i == 0 || continued) text(content, fonts, true, 9, 52, y, row.label);
                text(content, fonts, false, 9, 235, y, valueLines.get(i)); y -= 12;
            }
            y -= 4;
        }
        @Override public void close() throws IOException {
            if (content != null) {
                try { text(content, fonts, false, 8, 52, 48, DISCLAIMER); }
                finally { content.close(); content = null; }
            }
        }
    }

    private void text(PDPageContentStream c, DocumentFonts fonts, boolean bold, float size, float x, float y, String value) throws IOException {
        String visual = fonts.visual(value);
        // ActualText retains the authoritative logical string for consumers supporting marked content.
        var properties = new org.apache.pdfbox.cos.COSDictionary();
        properties.setString(org.apache.pdfbox.cos.COSName.ACTUAL_TEXT, value == null || value.isEmpty() ? "-" : value);
        c.beginMarkedContent(org.apache.pdfbox.cos.COSName.getPDFName("Span"), org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList.create(properties));
        c.beginText(); c.newLineAtOffset(x, y);
        PDFont current = null; StringBuilder run = new StringBuilder();
        for (int cp : visual.codePoints().toArray()) {
            PDFont selected = fonts.font(cp, bold);
            if (selected != current && !run.isEmpty()) { c.setFont(current, size); c.showText(run.toString()); run.setLength(0); }
            current = selected; run.appendCodePoint(cp);
        }
        if (!run.isEmpty()) { c.setFont(current, size); c.showText(run.toString()); }
        c.endText(); c.endMarkedContent();
    }
    private List<String> lines(String value, DocumentFonts fonts, float size, float width) throws IOException {
        String original = value == null || value.isEmpty() ? "-" : value;
        fonts.visual(original); // Fail before emitting a partial field; never normalize or truncate names.
        List<String> lines = new ArrayList<>(); StringBuilder line = new StringBuilder();
        for (int cp : original.codePoints().toArray()) {
            String candidate = line.toString() + Character.toString(cp);
            if (!line.isEmpty() && fonts.width(candidate, false, size) > width) {
                lines.add(line.toString()); line.setLength(0);
            }
            line.appendCodePoint(cp);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
    private String eur(BigDecimal value) { return "EUR " + value.setScale(2).toPlainString(); }
    private String minutes(Integer value) { return value + " minutes"; }
    private record Row(String label, String value, boolean section) {
        Row(String label, String value) { this(label, value, false); }
        static Row section(String label) { return new Row(label, null, true); }
    }
}
