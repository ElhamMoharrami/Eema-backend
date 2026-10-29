package com.eema.backend.controller;

import com.eema.backend.dto.request.MatchRequest;
import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.service.MatchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse> search(
            @Valid @RequestBody MatchRequest request) {

        log.info(
                "Match request received: briefLength={}, venueId={}",
                request.brief().length(),
                request.venueId()
        );

        MatchResponse response = matchService.findMatches(request);

        return ResponseEntity.ok(response);
    }
}