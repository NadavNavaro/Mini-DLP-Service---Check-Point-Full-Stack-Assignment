package com.nadav.dlp.dataset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/datasets")
public class DataSetController {
    private final DataSetService service;
    public DataSetController(DataSetService service) { this.service = service; }
    @GetMapping
    public List<DataSetDto.Response> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) { return service.list(page, size); }
    @GetMapping("/{id}")
    public DataSetDto.Response get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<DataSetDto.Response> create(@Valid @RequestBody DataSetDto.Request input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/datasets/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public DataSetDto.Response update(@PathVariable UUID id, @Valid @RequestBody DataSetDto.Request input) {
        return service.update(id, input);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
