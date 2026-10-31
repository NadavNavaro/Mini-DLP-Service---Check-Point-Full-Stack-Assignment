package com.nadav.dlp.scan;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Accepts validated text and a policy ID, then returns a synchronous inspection result. */
@RestController
@RequestMapping("/api/scan")
public class ScanController {
    private final ScanService service;
    public ScanController(ScanService service) { this.service = service; }
    @PostMapping
    public ScanDto.Response scan(@Valid @RequestBody ScanDto.Request input) { return service.scan(input); }
}
