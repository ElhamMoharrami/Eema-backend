package com.eema.backend.dto.request;

import com.eema.backend.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit contract table: expected boundaries are not derived from production annotations. */
@JsonTest @Import(JacksonConfig.class)
class RequestBoundaryContractTest {
 @Autowired ObjectMapper json;
 static final ValidatorFactory VALIDATORS=Validation.buildDefaultValidatorFactory();
 @AfterAll static void close(){VALIDATORS.close();}
 static final String UUID_VALUE="00000000-0000-4000-8000-000000000001", DAY="2027-06-15", TIME="09:00", END="10:00";
 static final Object OMIT=new Object();
 record Case(String label,Class<?> type,Map<String,Object> body,boolean valid) {public String toString(){return label;}}
 static final Map<Class<?>,Map<String,Object>> BASE=new LinkedHashMap<>();
 static final List<Case> CASES=new ArrayList<>();
 static Map<String,Object> map(Object... pairs){var m=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
 static void base(Class<?> type,Object... pairs){BASE.put(type,map(pairs));}
 static void check(Class<?> type,String field,Object value,boolean valid,String label){var m=new LinkedHashMap<>(BASE.get(type));if(value==OMIT)m.remove(field);else m.put(field,value);CASES.add(new Case(type.getSimpleName()+"."+field+" "+label,type,m,valid));}
 static void required(Class<?> type,String... fields){for(String f:fields){check(type,f,null,false,"null");check(type,f,OMIT,false,"omitted");}}
 static void optional(Class<?> type,String... fields){for(String f:fields){check(type,f,null,true,"null optional");check(type,f,OMIT,true,"omitted optional");}}
 static void text(Class<?> t,String f,int min,int max,boolean required){
  if(required)required(t,f);else optional(t,f);
  check(t,f,"",!required&&min==0,"empty");check(t,f,"   ",!required,"blank");
  check(t,f,"x".repeat(Math.max(1,min)),true,"minimum");if(min>1)check(t,f,"x".repeat(min-1),false,"below minimum");
  if(max>0){check(t,f,"x".repeat(max),true,"maximum");check(t,f,"x".repeat(max+1),false,"above maximum");}
 }
 static void number(Class<?> t,String f,String min,String max,boolean required,boolean decimal){
  if(required)required(t,f);else optional(t,f);BigDecimal lo=new BigDecimal(min),hi=new BigDecimal(max),step=new BigDecimal(decimal?"0.01":"1");
  for(var pair:List.of(Map.entry("minimum",lo),Map.entry("maximum",hi),Map.entry("below",lo.subtract(step)),Map.entry("above",hi.add(step))))check(t,f,pair.getValue(),!Set.of("below","above").contains(pair.getKey()),pair.getKey());
  if(!decimal)check(t,f,new BigDecimal("999999999999999999999"),false,"integer overflow");
 }
 static void collection(Class<?> t,String f,int min,int max,boolean required,Object item,boolean nullElementInvalid){
  if(required)required(t,f);else optional(t,f);
  check(t,f,List.of(),min==0,"empty collection");check(t,f,List.of(item),true,"one element");
  // Distinct strings/UUIDs for Set semantics; enum duplicates are DTO-valid and service-rejected separately.
  var items=new ArrayList<>();for(int i=0;i<max+1;i++)items.add(t==AcceptPoliciesRequest.class?new UUID(0,i+1).toString():item);
  check(t,f,items.subList(0,max),true,"maximum count");check(t,f,items,false,"above maximum count");check(t,f,Arrays.asList((Object)null),!nullElementInvalid,"null element");
 }
 static {
  var contact=map("name","QA Scout","email","qa@example.test","phone","123");var interval=map("dayOfWeek","MONDAY","startTime",TIME,"endTime",END);
  base(AcceptPoliciesRequest.class,"policyVersionIds",List.of(UUID_VALUE));base(CancelBookingRequest.class,"reason","QA reason","previewToken","synthetic-proof");
  base(CreateAvailabilityInquiryRequest.class,"venueId",UUID_VALUE,"requestedDate",DAY,"requestedStartTime",TIME,"requestedEndTime",END,"message","QA inquiry");
  base(CreateAvailabilityOverrideRequest.class,"date",DAY,"startTime",TIME,"endTime",END,"type","AVAILABLE","allDay",false);
  base(CreateBookingRequest.class,"venueId",UUID_VALUE,"shootDate",DAY,"startTime",TIME,"endTime",END,"rawBrief","QA booking","scoutContact",contact);
  base(CreateIncidentRequest.class,"category","OTHER","description","QA incident");base(CreatePayoutDestinationRequest.class,"accountHolderName","QA Owner","iban","synthetic-unvalidated-iban");
  base(CreatePolicyVersionRequest.class,"policyType","TERMS_OF_SERVICE","version","QA1","title","QA terms","effectiveAt","2027-06-15T09:00:00Z","content","QA NOT LEGAL TERMS");
  var venue=map("name","QA Studio","neighbourhood","QA Area","sizeM2",100,"hourlyRateEur",125.50,"minimumBookingMinutes",60,"minimumLeadTimeMinutes",60,"fullDayRateEur",900,"maxCrewSize",20,"permittedActivities",List.of("FILM"),"prohibitedActivities",List.of("PHOTO"),"ceilingHeightM",3,"parkingSpaces",2,"floorNumber",0,"productionRules","QA rules","powerDetails","QA power","tags",List.of("qa"),"description","QA description","clipUrl","https://example.test/clip");
  for(String f:List.of("naturalLight","blackoutPossible","loadingAccess","parkingAvailable","liftAvailable","wheelchairAccessible","bathroomsAvailable","soundRecordingSuitable"))venue.put(f,true);
  BASE.put(CreateVenueRequest.class,venue);var update=new LinkedHashMap<>(venue);update.put("timezone","Europe/Amsterdam");update.put("photoPaths",List.of("https://example.test/qa.png"));BASE.put(UpdateVenueRequest.class,update);
  base(EmailAddressRequest.class,"email","qa@example.test");base(LoginRequest.class,"email","qa@example.test","password","QA-password!");base(MarkOwnerPayoutPaidRequest.class,"externalTransferReference","QA-record");base(MatchRequest.class,"brief","QA filming brief","venueId",UUID_VALUE);
  base(OfferAvailabilityInquiryRequest.class,"offeredDate",DAY,"offeredStartTime",TIME,"offeredEndTime",END);base(OptionalNoteRequest.class,"note","QA note");base(OwnerVerificationApproveRequest.class,"verificationMethod","MANUAL_REVIEW","note","QA note");base(OwnerVerificationSubmitRequest.class,"note","QA note");base(ReasonRequest.class,"reason","QA reason");base(RefreshRequest.class,"refreshToken","synthetic-token");base(RegisterRequest.class,"email","qa@example.test","password","QA-password!","fullName","QA Scout","role","SCOUT");base(ResetPasswordRequest.class,"token","synthetic-token","newPassword","QA-password!");base(ResolveIncidentRequest.class,"resolutionNote","QA resolution");BASE.put(ScoutContactRequest.class,contact);base(UpdateVenueAvailabilitySettingsRequest.class,"timezone","Europe/Amsterdam");base(UpdateWeeklyAvailabilityRequest.class,"intervals",List.of(interval));base(VerifyEmailRequest.class,"token","synthetic-token");BASE.put(WeeklyIntervalRequest.class,interval);
  for(var e:BASE.entrySet()){
   CASES.add(new Case(e.getKey().getSimpleName()+" valid baseline",e.getKey(),e.getValue(),true));
   check(e.getKey(),"unexpected",true,!StrictAuthRequest.class.isAssignableFrom(e.getKey()),"unknown field scope");
   for(String f:e.getValue().keySet())check(e.getKey(),f,List.of(Map.of("invalid",true)),false,"incompatible JSON shape");
  }
  collection(AcceptPoliciesRequest.class,"policyVersionIds",1,5,true,UUID_VALUE,true);
  text(CancelBookingRequest.class,"reason",0,1000,false);text(CancelBookingRequest.class,"previewToken",0,2048,false);
  required(CreateAvailabilityInquiryRequest.class,"venueId","requestedDate","requestedStartTime","requestedEndTime");text(CreateAvailabilityInquiryRequest.class,"message",1,1000,true);
  required(CreateAvailabilityOverrideRequest.class,"date","type");optional(CreateAvailabilityOverrideRequest.class,"startTime","endTime","allDay");
  required(CreateBookingRequest.class,"venueId","shootDate","startTime","endTime","scoutContact");text(CreateBookingRequest.class,"rawBrief",1,1000,true);check(CreateBookingRequest.class,"scoutContact",map("name","x","email","invalid"),false,"nested invalid contact");
  required(CreateIncidentRequest.class,"category");text(CreateIncidentRequest.class,"description",1,4000,true);check(CreateIncidentRequest.class,"description","<b>markup</b>",false,"markup");
  text(CreatePayoutDestinationRequest.class,"accountHolderName",1,160,true);text(CreatePayoutDestinationRequest.class,"iban",1,80,true);
  required(CreatePolicyVersionRequest.class,"policyType","effectiveAt");text(CreatePolicyVersionRequest.class,"version",1,50,true);text(CreatePolicyVersionRequest.class,"title",1,200,true);text(CreatePolicyVersionRequest.class,"content",1,200000,true);
  for(Class<?> t:List.of(CreateVenueRequest.class,UpdateVenueRequest.class)){
   text(t,"name",2,120,true);text(t,"neighbourhood",1,100,true);number(t,"sizeM2","1","9999",true,false);number(t,"hourlyRateEur","0.01","99999999.99",true,true);number(t,"minimumBookingMinutes","1","10080",true,false);number(t,"minimumLeadTimeMinutes","60","43200",true,false);number(t,"fullDayRateEur","0.01","99999999.99",false,true);number(t,"maxCrewSize","1","10000",false,false);number(t,"ceilingHeightM","0.01","99.99",false,true);number(t,"parkingSpaces","0","10000",false,false);number(t,"floorNumber","-20","200",false,false);
   for(String f:List.of("naturalLight","blackoutPossible","loadingAccess","parkingAvailable","liftAvailable","wheelchairAccessible","bathroomsAvailable","soundRecordingSuitable")){optional(t,f);check(t,f,false,true,"false preserved");check(t,f,true,true,"true preserved");}
   collection(t,"permittedActivities",0,7,false,"FILM",true);collection(t,"prohibitedActivities",0,7,false,"PHOTO",true);text(t,"productionRules",0,5000,false);text(t,"powerDetails",0,2000,false);text(t,"description",0,5000,false);text(t,"clipUrl",0,500,false);collection(t,"tags",0,20,true,"qa",true);check(t,"tags",List.of(" "),false,"blank tag");check(t,"tags",List.of("x".repeat(50)),true,"tag length50");check(t,"tags",List.of("x".repeat(51)),false,"tag length51");
  }
  for(Class<?> t:List.of(EmailAddressRequest.class,LoginRequest.class,RegisterRequest.class,ScoutContactRequest.class)){
   required(t,"email");check(t,"email","",false,"empty email");check(t,"email","not-an-email",false,"malformed email");
   if(t!=LoginRequest.class){int max=t==ScoutContactRequest.class?255:254;check(t,"email",email(max),true,"email maximum");check(t,"email",email(max+1),false,"email above maximum");}
  }
  text(LoginRequest.class,"password",8,72,true);text(RegisterRequest.class,"password",8,72,true);text(RegisterRequest.class,"fullName",2,120,true);required(RegisterRequest.class,"role");text(ResetPasswordRequest.class,"newPassword",8,72,true);text(ResetPasswordRequest.class,"token",1,0,true);text(RefreshRequest.class,"refreshToken",1,0,true);text(VerifyEmailRequest.class,"token",1,0,true);
  text(MarkOwnerPayoutPaidRequest.class,"externalTransferReference",1,200,true);text(MatchRequest.class,"brief",10,1000,true);optional(MatchRequest.class,"venueId");required(OfferAvailabilityInquiryRequest.class,"offeredDate","offeredStartTime","offeredEndTime");
  text(OptionalNoteRequest.class,"note",0,1000,false);text(OwnerVerificationSubmitRequest.class,"note",0,1000,false);text(OwnerVerificationApproveRequest.class,"note",0,1000,false);required(OwnerVerificationApproveRequest.class,"verificationMethod");text(ReasonRequest.class,"reason",1,1000,true);text(ResolveIncidentRequest.class,"resolutionNote",1,2000,true);check(ResolveIncidentRequest.class,"resolutionNote","a > b",false,"markup");
  text(ScoutContactRequest.class,"name",2,100,true);text(ScoutContactRequest.class,"phone",0,50,false);text(UpdateVenueAvailabilitySettingsRequest.class,"timezone",1,0,true);text(UpdateVenueRequest.class,"timezone",1,100,true);collection(UpdateVenueRequest.class,"photoPaths",0,12,true,"https://example.test/qa.png",true);check(UpdateVenueRequest.class,"photoPaths",List.of("x".repeat(500)),true,"photo length500");check(UpdateVenueRequest.class,"photoPaths",List.of("x".repeat(501)),false,"photo length501");check(UpdateVenueRequest.class,"photoPaths",List.of(" "),false,"blank photo");
  collection(UpdateWeeklyAvailabilityRequest.class,"intervals",0,50,true,interval,true);check(UpdateWeeklyAvailabilityRequest.class,"intervals",List.of(map("dayOfWeek","MONDAY")),false,"nested missing times");required(WeeklyIntervalRequest.class,"dayOfWeek","startTime","endTime");
  for(Class<?> t:BASE.keySet())for(var c:t.getRecordComponents()){
   String f=c.getName();if(c.getType().isEnum()){check(t,f,"UNKNOWN",false,"unknown enum");for(Object e:c.getType().getEnumConstants())check(t,f,e.toString(),true,"enum "+e);}
   if(c.getType()==java.time.LocalDate.class)check(t,f,"2027-02-30",false,"invalid date");if(c.getType()==java.time.LocalTime.class)check(t,f,"25:00",false,"invalid time");if(c.getType()==UUID.class)check(t,f,"not-a-uuid",false,"invalid UUID");
  }
 }
 static String email(int length){return "a".repeat(64)+"@"+"b".repeat(63)+"."+"c".repeat(63)+"."+"d".repeat(length-196)+".nl";}
 static Stream<Arguments> boundaries(){return CASES.stream().map(c->Arguments.of(c));}
 @ParameterizedTest(name="{0}") @MethodSource("boundaries") void boundaries(Case c)throws Exception{
  Object bound;
  try{bound=json.readValue(json.writeValueAsBytes(c.body()),c.type());}
  catch(com.fasterxml.jackson.core.JsonProcessingException ex){assertFalse(c.valid(),c.label()+" binding unexpectedly rejected");return;}
  assertEquals(c.valid(),VALIDATORS.getValidator().validate(bound).isEmpty(),c.label());
 }
 @Test void tableCoversEveryRecordField(){assertEquals(28,BASE.size());for(var e:BASE.entrySet())assertEquals(Arrays.stream(e.getKey().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()),e.getValue().keySet(),e.getKey().getSimpleName());}
}
