package com.eema.backend.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Explicit inventoried API surface; domain suites prove allowed operations and object/state semantics. */
class ApiSurfaceIntegrationTest extends BookingCoreSupport {
 @BeforeAll static void fresh(){owner=null;policies.clear();}
 @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
 static final List<String> ROUTES=List.of(
  "API-001 GET /admin/bookings",
  "API-002 GET /admin/bookings/{id}",
  "API-003 GET /admin/bookings/{id}/events",
  "API-004 GET /admin/incidents",
  "API-005 GET /admin/incidents/{id}",
  "API-006 POST /admin/incidents/{id}/start-review",
  "API-007 POST /admin/incidents/{id}/resolve",
  "API-008 POST /admin/incidents/{id}/close",
  "API-009 GET /admin/operations/exceptions",
  "API-010 GET /admin/liabilities",
  "API-011 GET /admin/liabilities/{id}",
  "API-012 POST /admin/liabilities/{id}/settle",
  "API-013 GET /admin/payouts",
  "API-014 GET /admin/payouts/{id}",
  "API-015 POST /admin/payouts/{id}/mark-paid",
  "API-016 GET /admin/owner-verifications",
  "API-017 GET /admin/owner-verifications/{id}",
  "API-018 POST /admin/owner-verifications/{id}/approve",
  "API-019 POST /admin/owner-verifications/{id}/reject",
  "API-020 GET /admin/payout-destinations",
  "API-021 GET /admin/payout-destinations/{id}",
  "API-022 POST /admin/payout-destinations/{id}/verify",
  "API-023 POST /admin/payout-destinations/{id}/reject",
  "API-024 POST /admin/payout-destinations/{id}/reveal",
  "API-025 GET /admin/policies",
  "API-026 POST /admin/policies",
  "API-027 POST /admin/policies/{id}/publish",
  "API-028 POST /admin/policies/{id}/retire",
  "API-029 GET /admin/venues",
  "API-030 GET /admin/venues/{id}",
  "API-031 GET /admin/venues/{id}/events",
  "API-032 POST /admin/venues/{id}/approve",
  "API-033 POST /admin/venues/{id}/request-changes",
  "API-034 POST /admin/venues/{id}/suspend",
  "API-035 POST /admin/venues/{id}/reactivate",
  "API-036 POST /auth/register",
  "API-037 POST /auth/login",
  "API-038 POST /auth/refresh",
  "API-039 POST /auth/verify-email",
  "API-040 POST /auth/resend-verification",
  "API-041 POST /auth/forgot-password",
  "API-042 POST /auth/reset-password",
  "API-043 GET /auth/me",
  "API-044 POST /availability-inquiries",
  "API-045 GET /availability-inquiries/mine",
  "API-046 GET /availability-inquiries/owned-venues",
  "API-047 POST /availability-inquiries/{id}/decline",
  "API-048 POST /availability-inquiries/{id}/offer",
  "API-049 POST /availability-inquiries/{id}/accept",
  "API-050 POST /bookings",
  "API-051 GET /bookings",
  "API-052 GET /bookings/{id}",
  "API-053 POST /bookings/{id}/accept",
  "API-054 POST /bookings/{id}/decline",
  "API-055 POST /bookings/{id}/cancel",
  "API-056 GET /bookings/{id}/cancellation-preview",
  "API-057 POST /bookings/{id}/refund",
  "API-058 POST /bookings/{bookingId}/checkout",
  "API-059 GET /bookings/{id}/receipt",
  "API-060 POST /bookings/{bookingId}/incidents",
  "API-061 GET /bookings/{bookingId}/incidents",
  "API-062 POST /match",
  "API-063 GET /owner/payouts",
  "API-064 GET /owner/payouts/{id}",
  "API-065 GET /owner/payouts/{id}/statement",
  "API-066 GET /owner/payout-destinations",
  "API-067 POST /owner/payout-destinations",
  "API-068 GET /owner/verification",
  "API-069 POST /owner/verification/submit",
  "API-070 GET /policies/current",
  "API-071 GET /policies/{type}/current",
  "API-072 GET /policies/status",
  "API-073 POST /policies/accept",
  "API-074 POST /upload/venue-photo",
  "API-075 GET /upload/files/{filename:.+}",
  "API-076 GET /venues/{venueId}/availability",
  "API-077 GET /venues/{venueId}/availability/settings",
  "API-078 PUT /venues/{venueId}/availability/settings",
  "API-079 PUT /venues/{venueId}/availability/weekly",
  "API-080 POST /venues/{venueId}/availability/overrides",
  "API-081 DELETE /venues/{venueId}/availability/overrides/{overrideId}",
  "API-082 GET /venues",
  "API-083 GET /venues/{id}",
  "API-084 POST /venues",
  "API-085 GET /venues/my",
  "API-086 PUT /venues/{id}",
  "API-087 POST /venues/{id}/photos",
  "API-088 POST /venues/{id}/submit-for-review",
  "API-089 GET /venues/{id}/moderation",
  "API-090 POST /webhooks/mollie/payment"
 );
 static Stream<String> adminRoutes(){return ROUTES.stream().filter(r->r.contains(" /admin/"));}
 Map<String,Long> counts(){var result=new LinkedHashMap<String,Long>();for(String table:List.of("bookings","booking_events","venues","owner_payouts","owner_financial_liabilities","owner_verifications","owner_payout_destinations","policy_versions","marketplace_trust_events","incident_reports","notification_outbox"))result.put(table,jdbc.queryForObject("select count(*) from "+table,Long.class));return result;}
 @ParameterizedTest @MethodSource("adminRoutes") void everyAdminRouteDeniesAnonymousScoutAndOwnerWithoutWrites(String route)throws Exception{
  String[] bits=route.split(" ",3);String path=bits[2].replace("{id}","00000000-0000-4000-8000-000000000001");var before=counts();
  for(var actor:Arrays.asList(null,scout,owner)){req(request(org.springframework.http.HttpMethod.valueOf(bits[1]),"/api"+path),actor,bits[1].equals("GET")?null:Map.of(),actor==null?401:403);assertEquals(before,counts(),route);}
  verifyNoInteractions(mollie);
 }
 @Test void explicitNinetyRouteInventoryMatchesAllApplicationControllers(){
  Set<String> actual=new TreeSet<>();mappings.getHandlerMethods().forEach((mapping,handler)->{if(handler.getBeanType().getPackageName().equals("com.eema.backend.controller"))for(var method:mapping.getMethodsCondition().getMethods())for(String path:mapping.getPatternValues())actual.add(method.name()+" "+path);});
  Set<String> expected=new TreeSet<>();for(String route:ROUTES)expected.add(route.substring(route.indexOf(' ')+1));assertEquals(90,expected.size());assertEquals(expected,actual);
 }
}
