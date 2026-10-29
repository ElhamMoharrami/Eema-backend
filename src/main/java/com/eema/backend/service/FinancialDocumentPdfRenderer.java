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
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 790;
                text(content, bold, 22, 52, y, "Eema"); y -= 34;
                text(content, bold, 17, 52, y, title); y -= 20;
                text(content, regular, 9, 52, y, "Document reference: " + number); y -= 28;
                for (Row row : rows) {
                    if (row.section) { y -= 8; text(content, bold, 12, 52, y, row.label); y -= 18; continue; }
                    text(content, bold, 9, 52, y, row.label);
                    List<String> valueLines = lines(row.value, 62);
                    for (int i = 0; i < valueLines.size(); i++) text(content, regular, 9, 235, y - (i * 12), valueLines.get(i));
                    y -= Math.max(16, valueLines.size() * 12);
                }
                text(content, regular, 8, 52, 48, DISCLAIMER);
            }
            document.getDocumentInformation().setTitle(title);
            document.getDocumentInformation().setSubject("Eema payment record");
            document.save(out); return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Could not render financial document", e); }
    }
    private void text(PDPageContentStream c, PDFont font, float size, float x, float y, String value) throws IOException {
        c.beginText(); c.setFont(font, size); c.newLineAtOffset(x, y); c.showText(safe(value)); c.endText();
    }
    private String safe(String value) {
        if (value == null || value.isBlank()) return "-";
        StringBuilder result = new StringBuilder();
        value.codePoints().limit(180).forEach(cp -> result.append(cp >= 32 && cp <= 255 ? (char) cp : '?'));
        return result.toString().replace('\n', ' ').replace('\r', ' ');
    }
    private List<String> lines(String value, int width) {
        String normalized = safe(value); List<String> lines = new ArrayList<>();
        for (int start = 0; start < normalized.length(); start += width)
            lines.add(normalized.substring(start, Math.min(start + width, normalized.length())));
        return lines.isEmpty() ? List.of("-") : lines;
    }
    private String eur(BigDecimal value) { return "EUR " + value.setScale(2).toPlainString(); }
    private String minutes(Integer value) { return value + " minutes"; }
    private record Row(String label, String value, boolean section) {
        Row(String label, String value) { this(label, value, false); }
        static Row section(String label) { return new Row(label, null, true); }
    }
}
