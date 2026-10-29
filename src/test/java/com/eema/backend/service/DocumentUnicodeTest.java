package com.eema.backend.service;

import com.eema.backend.entity.*;
import java.nio.file.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DocumentUnicodeTest {
 final DocumentRendererBoundaryTest fixtures = new DocumentRendererBoundaryTest();
 final FinancialDocumentPdfRenderer renderer=fixtures.renderer;
 Booking booking(){return fixtures.booking();}
 Venue venue(){return fixtures.venue();}
 String text(byte[] bytes)throws Exception{return fixtures.text(bytes);}
 @ParameterizedTest @ValueSource(strings={"Jane Smith","José García","Zoë François","Daniël Müller","Łukasz Żółć","Νίκος","Алексей","李雷","محمد","李雷 García"})
 void storedNamesSurviveExtraction(String name)throws Exception{
  var b=booking();b.setScoutContact(BookingContactSnapshot.builder().name(name).email("qa@example.test").build());
  byte[] bytes=renderer.bookingReceipt("RCT-UNICODE",b,venue());
  assertTrue(text(bytes).contains(name),()->"Name missing from extraction: "+name);
  if(Boolean.getBoolean("eema.doc.samples")){Path dir=Path.of("/tmp/eema-unicode-samples");Files.createDirectories(dir);Files.write(dir.resolve("receipt-"+Integer.toHexString(name.hashCode())+".pdf"),bytes);}
  try(var pdf=Loader.loadPDF(bytes)){for(var page:pdf.getPages())for(var key:page.getResources().getFontNames())assertTrue(page.getResources().getFont(key).isEmbedded());}
 }
 @Test void longUnicodeNamePreservesEveryCharacterWithinBounds()throws Exception{
  var b=booking();String name="李雷 García ".repeat(10);b.setScoutContact(BookingContactSnapshot.builder().name(name).email("qa@example.test").build());byte[] bytes=renderer.bookingReceipt("RCT-LONG-UNICODE",b,venue());
  fixtures.sample("long-unicode-receipt",bytes);
  assertTrue(text(bytes).replaceAll("\\s+","").contains(name.replaceAll("\\s+","")));
  try(var pdf=Loader.loadPDF(bytes)){new PDFTextStripper(){@Override protected void processTextPosition(TextPosition t){assertTrue(t.getXDirAdj()+t.getWidthDirAdj()<pdf.getPage(getCurrentPageNo()-1).getMediaBox().getWidth()-30);super.processTextPosition(t);}}.getText(pdf);}
 }
 @ParameterizedTest @ValueSource(strings={"Unknown \u0378", "Movie 🎬", "Control \u0085", "مُحَمَّد", "e\u0301", "Join\u200dControl"})
 void unsupportedTextFailsInsteadOfChangingIdentity(String name){var b=booking();b.setScoutContact(BookingContactSnapshot.builder().name(name).email("qa@example.test").build());assertThrows(IllegalStateException.class,()->renderer.bookingReceipt("RCT-UNSUPPORTED",b,venue()));}
 @Test void markupIsLiteralAndInternalSpacesArePreserved()throws Exception{
  String name="<b>李雷  García</b> &amp; **QA**";var b=booking();b.setScoutContact(BookingContactSnapshot.builder().name(name).email("qa@example.test").build());byte[] bytes=renderer.bookingReceipt("RCT-PLAIN-TEXT",b,venue());assertTrue(text(bytes).contains(name));
 }

}
