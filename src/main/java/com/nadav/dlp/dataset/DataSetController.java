package com.nadav.dlp.dataset;

import com.nadav.dlp.dataset.dto.*;

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
    public ResponseEntity<List<DataSetResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        var result = service.list(page, size);
        return ResponseEntity.ok().header("X-Total-Count", Long.toString(result.getTotalElements()))
            .header("X-Page", Integer.toString(page)).header("X-Page-Size", Integer.toString(size))
            .body(result.getContent());
    }
    @GetMapping("/{id}")
    public DataSetResponse get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<DataSetResponse> create(@Valid @RequestBody DataSetRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/datasets/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public DataSetResponse update(@PathVariable UUID id, @Valid @RequestBody DataSetRequest input) {
        return service.update(id, input);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
