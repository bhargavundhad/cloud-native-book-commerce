package com.bookecommerce.product_service.controller;

import com.bookecommerce.product_service.dto.LoanCreateRequest;
import com.bookecommerce.product_service.dto.LoanResponse;
import com.bookecommerce.product_service.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(request));
    }

    @GetMapping("/{id}")
    public LoanResponse findById(@PathVariable UUID id) {
        return loanService.findById(id);
    }

    @GetMapping("/borrower/{borrowerId}")
    public List<LoanResponse> findByBorrower(@PathVariable UUID borrowerId) {
        return loanService.findByBorrower(borrowerId);
    }

    @GetMapping("/listing/{listingId}")
    public List<LoanResponse> findByListing(@PathVariable UUID listingId) {
        return loanService.findByListing(listingId);
    }

    @PostMapping("/{id}/activate")
    public LoanResponse activate(@PathVariable UUID id) {
        return loanService.activate(id);
    }

    @PostMapping("/{id}/return")
    public LoanResponse returnLoan(@PathVariable UUID id) {
        return loanService.returnLoan(id);
    }

    @PostMapping("/{id}/cancel")
    public LoanResponse cancel(@PathVariable UUID id) {
        return loanService.cancel(id);
    }
}