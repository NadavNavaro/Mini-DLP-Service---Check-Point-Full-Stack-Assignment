package com.nadav.dlp.datatype;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/datatypes")
public class DataTypeController {
    private final DataTypeService service;
    public DataTypeController(DataTypeService service) { this.service = service; }
    @GetMapping
    public List<DataTypeDto.Response> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) { return service.list(page, size); }
    @GetMapping("/{id}")
    public DataTypeDto.Response get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<DataTypeDto.Response> create(@Valid @RequestBody DataTypeDto.Request input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/datatypes/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public DataTypeDto.Response update(@PathVariable UUID id, @Valid @RequestBody DataTypeDto.Request input) {
        return service.update(id, input);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
