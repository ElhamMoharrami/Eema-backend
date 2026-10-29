package com.eema.backend.service;
import com.eema.backend.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Independent expected vocabulary, including historical/internal values without inventing transitions. */
@JsonTest @Import(JacksonConfig.class)
class EnumRepresentationContractTest {
 @Autowired ObjectMapper json;
 static Stream<Arguments> contracts(){return Stream.of(
  Arguments.of("STATE-001", "com.eema.backend.entity.enums.ActorType", "SCOUT,OWNER,ADMIN,SYSTEM,WEBHOOK"),
  Arguments.of("STATE-002", "com.eema.backend.entity.enums.AvailabilityInquiryStatus", "OPEN,DECLINED,OFFERED,EXPIRED,ACCEPTED"),
  Arguments.of("STATE-003", "com.eema.backend.entity.enums.AvailabilityOverrideType", "AVAILABLE,BLOCKED"),
  Arguments.of("STATE-004", "com.eema.backend.entity.enums.BookingEventType", "REQUEST_SUBMITTED,REQUEST_ACCEPTED,REQUEST_DECLINED,REQUEST_EXPIRED,REQUEST_BECAME_UNAVAILABLE,APPROVAL_EXPIRED,BOOKING_CONFIRMED,PAYMENT_SUCCEEDED,PAYMENT_REFUNDED,PAYMENT_RECONCILED,REFUND_REQUESTED,REFUND_STATUS_CHANGED,LATE_PAYMENT_DETECTED,LATE_PAYMENT_REFUND_REQUESTED,LATE_PAYMENT_REFUND_COMPLETED,SCOUT_CANCELLED,OWNER_CANCELLED,ADMIN_CANCELLED,PAYOUT_CALCULATED,PAYOUT_READY,PAYOUT_MARKED_PAID,PAYOUT_CANCELLED,OWNER_LIABILITY_CREATED,OWNER_LIABILITY_SETTLED,BOOKING_RECEIPT_ISSUED,OWNER_PAYOUT_STATEMENT_ISSUED"),
  Arguments.of("STATE-005", "com.eema.backend.entity.enums.BookingStatus", "REQUESTED,AWAITING_PAYMENT,CONFIRMED,DECLINED,EXPIRED,UNAVAILABLE,CANCELLED,REFUNDED"),
  Arguments.of("STATE-006", "com.eema.backend.entity.enums.CancellationPolicyTier", "PRE_PAYMENT_WITHDRAWAL,GRACE_PERIOD,FLEXIBLE_FULL_REFUND,PARTIAL_REFUND,NON_REFUNDABLE,OWNER_FULL_REFUND,ADMIN_FULL_REFUND"),
  Arguments.of("STATE-007", "com.eema.backend.entity.enums.FinancialDocumentStatus", "ISSUED"),
  Arguments.of("STATE-008", "com.eema.backend.entity.enums.FinancialDocumentType", "BOOKING_RECEIPT,OWNER_PAYOUT_STATEMENT"),
  Arguments.of("STATE-009", "com.eema.backend.entity.enums.IncidentCategory", "PROPERTY_DAMAGE,ACCESS_PROBLEM,SAFETY,NOISE_NEIGHBOUR,MISREPRESENTATION,CONDUCT,OTHER"),
  Arguments.of("STATE-010", "com.eema.backend.entity.enums.IncidentStatus", "OPEN,UNDER_REVIEW,RESOLVED,CLOSED"),
  Arguments.of("STATE-011", "com.eema.backend.entity.enums.OperationalExceptionSeverity", "INFO,WARNING,CRITICAL"),
  Arguments.of("STATE-012", "com.eema.backend.entity.enums.OperationalExceptionType", "PAYOUT_READY_OVERDUE,PAYOUT_HELD,OWNER_LIABILITY_OPEN,REFUND_STUCK,PAYMENT_CREATION_STUCK,MOLLIE_FEE_UNRECONCILED,LATE_PAYMENT_REFUND_IN_PROGRESS,NOTIFICATION_DELIVERY_FAILED"),
  Arguments.of("STATE-013", "com.eema.backend.entity.enums.OwnerFinancialLiabilityStatus", "OPEN,SETTLED,WAIVED"),
  Arguments.of("STATE-014", "com.eema.backend.entity.enums.OwnerFinancialLiabilityType", "PROVIDER_COST,OWNER_PAYOUT_RECOVERY"),
  Arguments.of("STATE-015", "com.eema.backend.entity.enums.OwnerPayoutHoldReason", "WAITING_FOR_BOOKING_END,WAITING_FOR_PROVIDER_COST,REFUND_IN_PROGRESS,WAITING_FOR_PAYOUT_DESTINATION"),
  Arguments.of("STATE-016", "com.eema.backend.entity.enums.OwnerPayoutStatus", "CALCULATED,HELD,READY,PAID,CANCELLED"),
  Arguments.of("STATE-017", "com.eema.backend.entity.enums.OwnerVerificationStatus", "UNVERIFIED,PENDING,VERIFIED,REJECTED"),
  Arguments.of("STATE-018", "com.eema.backend.entity.enums.PaymentCreationStatus", "NOT_STARTED,CREATING,CREATED"),
  Arguments.of("STATE-019", "com.eema.backend.entity.enums.PayoutDestinationStatus", "PENDING_VERIFICATION,VERIFIED,REJECTED,REPLACED"),
  Arguments.of("STATE-020", "com.eema.backend.entity.enums.PayoutDestinationType", "IBAN"),
  Arguments.of("STATE-021", "com.eema.backend.entity.enums.PolicyStatus", "DRAFT,PUBLISHED,RETIRED"),
  Arguments.of("STATE-022", "com.eema.backend.entity.enums.PolicyType", "TERMS_OF_SERVICE,PRIVACY_POLICY,ACCEPTABLE_USE_POLICY,PRODUCTION_POLICY,INSURANCE_REQUIREMENTS"),
  Arguments.of("STATE-023", "com.eema.backend.entity.enums.ProductionActivity", "PHOTO,FILM,VIDEO,INTERVIEW,COMMERCIAL,MUSIC_VIDEO,CONTENT_CREATION"),
  Arguments.of("STATE-024", "com.eema.backend.entity.enums.ProviderCostResponsibility", "NONE,OWNER_SETTLEMENT,OWNER,EEMA"),
  Arguments.of("STATE-025", "com.eema.backend.entity.enums.RefundStatus", "NONE,CREATING,QUEUED,PENDING,PROCESSING,REFUNDED,FAILED,CANCELED"),
  Arguments.of("STATE-026", "com.eema.backend.entity.enums.TrustEventType", "OWNER_VERIFICATION_SUBMITTED,OWNER_VERIFICATION_APPROVED,OWNER_VERIFICATION_REJECTED,VENUE_SUBMITTED,VENUE_APPROVED,VENUE_CHANGES_REQUESTED,VENUE_SUSPENDED,VENUE_REACTIVATED,PAYOUT_DESTINATION_SUBMITTED,PAYOUT_DESTINATION_VERIFIED,PAYOUT_DESTINATION_REJECTED,PAYOUT_DESTINATION_REPLACED,PAYOUT_DESTINATION_REVEALED,PAYOUT_DESTINATION_SNAPSHOTTED,INCIDENT_REPORTED,INCIDENT_REVIEW_STARTED,INCIDENT_RESOLVED,INCIDENT_CLOSED"),
  Arguments.of("STATE-027", "com.eema.backend.entity.enums.TrustSubjectType", "OWNER_VERIFICATION,VENUE,PAYOUT_DESTINATION,INCIDENT"),
  Arguments.of("STATE-028", "com.eema.backend.entity.enums.UserRole", "SCOUT,OWNER,ADMIN"),
  Arguments.of("STATE-029", "com.eema.backend.entity.enums.VenueStatus", "DRAFT,PENDING,ACTIVE,SUSPENDED"),
  Arguments.of("STATE-030", "com.eema.backend.entity.enums.VerificationMethod", "MANUAL_REVIEW,VIDEO_CALL,BUSINESS_DOCUMENT,VENUE_CONTROL_EVIDENCE"),
  Arguments.of("STATE-031", "com.eema.backend.event.BookingLifecycleEvent$Type", "REQUEST_SUBMITTED,REQUEST_ACCEPTED,REQUEST_DECLINED,REQUEST_EXPIRED,REQUEST_BECAME_UNAVAILABLE,APPROVAL_EXPIRED"),
  Arguments.of("STATE-032", "com.eema.backend.event.AvailabilityInquiryLifecycleEvent$Type", "CREATED,OFFERED,DECLINED,OFFER_EXPIRED"),
  Arguments.of("STATE-033", "com.eema.backend.service.BookingService$PaidPaymentResult", "APPLIED,LATE_PAYMENT,DUPLICATE")
 );}
 @ParameterizedTest(name="{0}") @MethodSource("contracts")
 void allDeclaredValuesRoundTripWithoutDefaulting(String scenario,String className,String vocabulary)throws Exception{
  Class<?> type=Class.forName(className);String[] expected=vocabulary.split(",");Object[] constants=type.getEnumConstants();assertNotNull(constants);assertArrayEquals(expected,Arrays.stream(constants).map(x->((Enum<?>)x).name()).toArray(String[]::new),scenario);
  for(Object constant:constants){String encoded=json.writeValueAsString(constant);assertEquals("\""+((Enum<?>)constant).name()+"\"",encoded);assertSame(constant,json.readValue(encoded,type));}
  assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,()->json.readValue("\"UNKNOWN_STATE\"",type));assertNull(json.readValue("null",type));
 }
}
