package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.SearchResponse;
import com.example.workflow_management_system.model.SearchType;
import com.example.workflow_management_system.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/search")
@Validated
@Tag(name = "Search", description = "Global Search endpoints")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SearchType type,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // If q is null, default empty string
        String query = (q == null) ? "" : q;

        return ResponseEntity.ok(searchService.search(query, type, pageable));
    }
}
