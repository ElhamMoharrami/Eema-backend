import com.eema.backend.service.FinancialDocumentPdfRenderer;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.BookingStatus;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Run through Spring Boot PropertiesLauncher: uses only the packaged app, never source-tree resources. */
public class DocumentFontPackageProbe {
 public static void main(String[] args)throws Exception{
  var renderer=new FinancialDocumentPdfRenderer();
  for(String name:List.of("José García","Zoë François","Daniël Müller","Łukasz Żółć","Νίκος","Алексей","李雷","محمد","李雷 García")){
   var booking=Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).paidAt(OffsetDateTime.parse("2026-09-19T12:00:00Z")).shootDate(LocalDate.of(2026,10,10)).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0)).scoutContact(BookingContactSnapshot.builder().name(name).email("synthetic@example.test").build()).hourlyRateEurSnapshot(new BigDecimal("125.50")).minimumBookingMinutesSnapshot(60).durationMinutes(60).billableMinutes(60).venuePriceEur(new BigDecimal("125.50")).totalPaidEur(new BigDecimal("125.50")).build();
   var venue=Venue.builder().name("QA Studio").neighbourhood("QA Area").timezone("Europe/Amsterdam").build();
   byte[] bytes=renderer.bookingReceipt("RCT-PACKAGE-PROBE",booking,venue);
   try(var pdf=Loader.loadPDF(bytes)){String text=new PDFTextStripper().getText(pdf);if(!text.contains(name)||!text.contains("125.50"))throw new AssertionError("Packaged name or money mismatch");for(var page:pdf.getPages())for(var id:page.getResources().getFontNames())if(!page.getResources().getFont(id).isEmbedded())throw new AssertionError("Unembedded font");}
   var owner=User.builder().fullName(name).email("owner@example.test").build();
   var payout=OwnerPayout.builder().id(UUID.randomUUID()).paidAt(booking.getPaidAt()).externalTransferReference("QA-packaged-transfer").grossRetainedAmountEur(new BigDecimal("125.50")).commissionEur(new BigDecimal("25.10")).providerCostEur(new BigDecimal("1.50")).payoutAmountEur(new BigDecimal("98.90")).payoutDestination(OwnerPayoutDestination.builder().ibanLast4("4821").build()).build();
   byte[] statement=renderer.payoutStatement("PST-PACKAGE-PROBE",payout,booking,venue,owner);
   try(var pdf=Loader.loadPDF(statement)){String text=new PDFTextStripper().getText(pdf);for(String expected:List.of(name,"125.50","25.10","1.50","98.90","••••••••••4821"))if(!text.contains(expected))throw new AssertionError("Packaged statement mismatch");for(var page:pdf.getPages())for(var id:page.getResources().getFontNames())if(!page.getResources().getFont(id).isEmbedded())throw new AssertionError("Unembedded statement font");}
   if(name.equals("محمد") && System.getProperty("eema.doc.probeOutput")!=null)java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("eema.doc.probeOutput")),statement);
  }
  System.out.println("PASS: packaged renderer, 9 original names in receipts and statements, exact money, mask and embedded fonts; no host fonts required");
 }
}
