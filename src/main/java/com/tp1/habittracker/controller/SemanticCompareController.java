package com.tp1.habittracker.controller;

import com.tp1.habittracker.dto.semantic.SemanticCompareRequest;
import com.tp1.habittracker.dto.semantic.SemanticCompareResponse;
import com.tp1.habittracker.service.SemanticCompareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic-compare")
@RequiredArgsConstructor
public class SemanticCompareController {

    private final SemanticCompareService semanticCompareService;

    @PostMapping
    public ResponseEntity<SemanticCompareResponse> compareTexts(@Valid @RequestBody SemanticCompareRequest request) {
        return ResponseEntity.ok(semanticCompareService.compareTexts(request));
    }
}
