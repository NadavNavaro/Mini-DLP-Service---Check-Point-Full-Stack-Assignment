package com.nadav.dlp.datatype;

import com.nadav.dlp.common.ApiException;
import com.nadav.dlp.dataset.DataSetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;

/** Owns rule transactions, keyword normalization and safe deletion of shared rules. */
@Service
@Transactional(readOnly = true)
public class DataTypeService {
    private final DataTypeRepository repository;
    private final DataSetRepository dataSets;

    public DataTypeService(DataTypeRepository repository, DataSetRepository dataSets) {
        this.repository = repository;
        this.dataSets = dataSets;
    }

    public List<DataTypeDto.Response> list(int page, int size) {
        // JPA offsets are signed integers even though PageRequest calculates them as long values.
        if ((long) page * size > Integer.MAX_VALUE) throw ApiException.invalid("Pagination offset is too large");
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).stream()
            .map(DataTypeDto.Response::from).toList();
    }
    public DataTypeDto.Response get(UUID id) { return DataTypeDto.Response.from(require(id)); }

    @Transactional
    public DataTypeDto.Response create(DataTypeDto.Request input) {
        var entity = new DataType(UUID.randomUUID());
        apply(entity, input);
        return DataTypeDto.Response.from(repository.save(entity));
    }

    /** Replaces a rule in one transaction, so every referencing policy sees the updated definition. */
    @Transactional
    public DataTypeDto.Response update(UUID id, DataTypeDto.Request input) {
        var entity = require(id);
        apply(entity, input);
        return DataTypeDto.Response.from(entity);
    }

    /** Rejects deletion while any policy references the rule; the database also protects concurrent races. */
    @Transactional
    public void delete(UUID id) {
        var entity = require(id);
        if (dataSets.existsByDataTypes_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Data Type is referenced by a Data Set; remove the reference first");
        }
        repository.delete(entity);
        repository.flush(); // Surface foreign-key races inside the exception handler.
    }

    private DataType require(UUID id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Data Type"));
    }

    /** Normalizes a complete replacement before mutating the entity; duplicates cannot inflate scan counts. */
    private void apply(DataType entity, DataTypeDto.Request input) {
        // Compare without case but preserve spelling: lowercasing can expand some Unicode characters.
        var unique = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        var keywords = new ArrayList<String>();
        for (String raw : input.content()) {
            String keyword = Normalizer.normalize(raw.strip(), Normalizer.Form.NFC);
            if (keyword.isBlank()) throw ApiException.invalid("Keywords must not be blank");
            if (keyword.length() > 100) throw ApiException.invalid("Normalized keywords must not exceed 100 characters");
            if (!unique.add(keyword)) throw ApiException.invalid("Duplicate keywords are not allowed (case-insensitive)");
            keywords.add(keyword);
        }
        if (input.name().strip().isBlank()) throw ApiException.invalid("Name must not be blank");
        entity.update(input.name().strip(), input.description().strip(), keywords, input.threshold());
    }
}
