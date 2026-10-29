package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.*;
import static org.junit.jupiter.api.Assertions.*;

class DocumentRendererBoundaryTest {
 final FinancialDocumentPdfRenderer renderer=new FinancialDocumentPdfRenderer();
 Booking booking(){return Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).shootDate(LocalDate.of(2026,10,10)).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0)).paidAt(OffsetDateTime.parse("2026-09-19T12:00:00Z")).scoutContact(BookingContactSnapshot.builder().name("QA "+"W".repeat(97)).email("qa-document@example.test").build()).hourlyRateEurSnapshot(new BigDecimal("99999999.99")).minimumBookingMinutesSnapshot(10080).durationMinutes(60).billableMinutes(10080).fullDayRateEurSnapshot(new BigDecimal("99999999.99")).venuePriceEur(new BigDecimal("99999999.99")).totalPaidEur(new BigDecimal("99999999.99")).build();}
 Venue venue(){return Venue.builder().id(UUID.randomUUID()).name("W".repeat(100)).neighbourhood("QA neighbourhood").timezone("Europe/Amsterdam").owner(User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).fullName("QA "+"W".repeat(97)).email("owner@example.test").build()).build();}
 void sample(String name,byte[] pdf)throws Exception{if(Boolean.getBoolean("eema.doc.samples")){Path dir=Path.of("/tmp/eema-doc-samples");Files.createDirectories(dir);Files.write(dir.resolve(name+".pdf"),pdf);}}
 String text(byte[] bytes)throws Exception{try(var p=Loader.loadPDF(bytes)){return new PDFTextStripper().getText(p);}}
 @Test void validWideNamesAndMaximumAmountsStayInsidePage()throws Exception{
  byte[] bytes=renderer.bookingReceipt("RCT-2026-QA-WIDE",booking(),venue());sample("wide-receipt",bytes);List<String> clipped=new ArrayList<>();
  try(var pdf=Loader.loadPDF(bytes)){new PDFTextStripper(){@Override protected void processTextPosition(TextPosition t){if(t.getXDirAdj()+t.getWidthDirAdj()>pdf.getPage(getCurrentPageNo()-1).getMediaBox().getWidth()-30||t.getYDirAdj()>pdf.getPage(getCurrentPageNo()-1).getMediaBox().getHeight()-30)clipped.add(t.getUnicode());super.processTextPosition(t);}}.getText(pdf);}
  assertTrue(clipped.isEmpty(),"Visible document text extends outside safe page bounds: "+clipped.size()+" glyphs");assertTrue(text(bytes).contains("99999999.99"));
 }
 @Test void completeTransferReferenceSurvivesRendering()throws Exception{
  var b=booking();var v=venue();String reference="QA-"+"X".repeat(193)+"-END";assertEquals(200,reference.length());
  var p=OwnerPayout.builder().id(UUID.randomUUID()).paidAt(b.getPaidAt()).externalTransferReference(reference).grossRetainedAmountEur(new BigDecimal("99999999.99")).commissionEur(new BigDecimal("19999999.99")).providerCostEur(new BigDecimal("0.01")).payoutAmountEur(new BigDecimal("79999999.99")).build();
  byte[] bytes=renderer.payoutStatement("PST-2026-QA-LONG",p,b,v,v.getOwner());sample("long-statement",bytes);assertTrue(text(bytes).replaceAll("\\s+","").contains(reference),"Persisted transfer reference must not be truncated");
 }
 @Test void unsupportedControlAndUnicodeInputFailsSafely() {
  var b=booking();b.setScoutContact(BookingContactSnapshot.builder().name("QA \u0085 李雷 🎬").email("qa@example.test").build());assertThrows(IllegalStateException.class,()->renderer.bookingReceipt("RCT-2026-QA-UNICODE",b,venue()));
 }
}
