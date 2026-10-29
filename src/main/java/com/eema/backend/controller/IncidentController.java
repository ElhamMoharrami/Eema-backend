package com.eema.backend.controller;
import com.eema.backend.dto.request.CreateIncidentRequest;
import com.eema.backend.dto.response.IncidentDto;
import com.eema.backend.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/bookings/{bookingId}/incidents") @RequiredArgsConstructor
public class IncidentController {
 private final IncidentService incidents;
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public IncidentDto report(@PathVariable UUID bookingId,@Valid @RequestBody CreateIncidentRequest r){return incidents.report(bookingId,r);}
 @GetMapping public List<IncidentDto> list(@PathVariable UUID bookingId){return incidents.forBooking(bookingId);}
}
