package com.tickethub.controller;

import com.tickethub.dto.SeatResponse;
import com.tickethub.dto.ShowRequest;
import com.tickethub.dto.ShowResponse;
import com.tickethub.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP for shows and the seat map. Listing is included so a created show and its 50 seats
 * can be read back. Booking is a later step.
 */
@RestController
@RequestMapping("/api")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping("/shows")
    public ResponseEntity<ShowResponse> create(@Valid @RequestBody ShowRequest request) {
        ShowResponse body = showService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/movies/{movieId}/shows")
    public List<ShowResponse> listByMovie(@PathVariable Long movieId) {
        return showService.findByMovie(movieId);
    }

    @GetMapping("/shows/{id}/seats")
    public List<SeatResponse> listSeats(@PathVariable Long id) {
        return showService.findSeats(id);
    }
}
