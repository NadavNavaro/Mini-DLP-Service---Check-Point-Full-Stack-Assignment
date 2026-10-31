package com.nadav.dlp.dataset;

import com.nadav.dlp.common.ApiException;
import com.nadav.dlp.datatype.DataTypeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Validates policy references and applies membership changes atomically within transactions. */
@Service
@Transactional(readOnly = true)
public class DataSetService {
    private final DataSetRepository repository;
    private final DataTypeRepository dataTypes;

    public DataSetService(DataSetRepository repository, DataTypeRepository dataTypes) {
        this.repository = repository;
        this.dataTypes = dataTypes;
    }
    public List<DataSetDto.Response> list(int page, int size) {
        // Reject an unrepresentable JPA offset as input error rather than an internal server error.
        if ((long) page * size > Integer.MAX_VALUE) throw ApiException.invalid("Pagination offset is too large");
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).stream()
            .map(DataSetDto.Response::from).toList();
    }
    public DataSetDto.Response get(UUID id) { return DataSetDto.Response.from(require(id)); }
    @Transactional
    public DataSetDto.Response create(DataSetDto.Request input) {
        var entity = new DataSet(UUID.randomUUID());
        apply(entity, input);
        return DataSetDto.Response.from(repository.saveAndFlush(entity));
    }
    @Transactional
    public DataSetDto.Response update(UUID id, DataSetDto.Request input) {
        var entity = require(id);
        apply(entity, input);
        repository.flush();
        return DataSetDto.Response.from(entity);
    }
    /** Deletes the policy and its join rows while preserving all reusable rules. */
    @Transactional
    public void delete(UUID id) { repository.delete(require(id)); }

    private DataSet require(UUID id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Data Set"));
    }
    /** Checks all IDs before changing policy membership, preserving the old state on invalid updates. */
    private void apply(DataSet entity, DataSetDto.Request input) {
        if (new HashSet<>(input.dataTypeIds()).size() != input.dataTypeIds().size()) {
            throw ApiException.invalid("Duplicate Data Type IDs are not allowed");
        }
        var types = dataTypes.findAllById(input.dataTypeIds());
        if (types.size() != input.dataTypeIds().size()) {
            throw ApiException.invalid("One or more referenced Data Types do not exist");
        }
        if (input.name().strip().isBlank()) throw ApiException.invalid("Name must not be blank");
        entity.update(input.name().strip(), types);
    }
}
