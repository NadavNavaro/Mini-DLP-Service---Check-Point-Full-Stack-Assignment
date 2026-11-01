package com.nadav.dlp.scan;

import com.nadav.dlp.scan.dto.*;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan")
public class ScanController {
    private final ScanService service;
    public ScanController(ScanService service) { this.service = service; }
    @PostMapping
    public ScanResponse scan(@Valid @RequestBody ScanRequest input) { return service.scan(input); }
}
