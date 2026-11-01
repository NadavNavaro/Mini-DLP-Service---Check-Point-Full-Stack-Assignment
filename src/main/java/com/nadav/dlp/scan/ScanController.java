package com.nadav.dlp.scan;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan")
public class ScanController {
    private final ScanService service;
    public ScanController(ScanService service) { this.service = service; }
    @PostMapping
    public ScanDto.Response scan(@Valid @RequestBody ScanDto.Request input) { return service.scan(input); }
}
