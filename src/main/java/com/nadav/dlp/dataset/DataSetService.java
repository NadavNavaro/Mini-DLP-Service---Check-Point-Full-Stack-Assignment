package com.nadav.dlp.dataset;

import com.nadav.dlp.dataset.dto.*;

import com.nadav.dlp.common.ApiException;
import com.nadav.dlp.datatype.DataTypeRepository;
import com.nadav.dlp.datatype.DataType;
import com.nadav.dlp.datatype.InspectionRule;
import org.springframework.data.domain.Page;
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
    public Page<DataSetResponse> list(int page, int size) {
        // Reject an unrepresentable JPA offset as input error rather than an internal server error.
        if ((long) page * size > Integer.MAX_VALUE) throw ApiException.invalid("Pagination offset is too large");
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).map(DataSetResponse::from);
    }
    /** Detaches the scan input from persistence before CPU-only matching begins. */
    public ScanPolicy loadForScan(UUID id) {
        var policy = repository.findForScan(id).orElseThrow(() -> ApiException.notFound("Data Set"));
        var rules = policy.getDataTypes().stream().sorted(Comparator.comparing(DataType::getId))
            .map(rule -> new InspectionRule(rule.getId(), rule.getName(), rule.getContent(), rule.getThreshold()))
            .toList();
        return new ScanPolicy(policy.getId(), policy.getName(), rules);
    }
    public DataSetResponse get(UUID id) { return DataSetResponse.from(require(id)); }
    @Transactional
    public DataSetResponse create(DataSetRequest input) {
        var entity = new DataSet(UUID.randomUUID());
        apply(entity, input);
        return DataSetResponse.from(repository.saveAndFlush(entity));
    }
    @Transactional
    public DataSetResponse update(UUID id, DataSetRequest input) {
        var entity = require(id);
        apply(entity, input);
        repository.flush();
        return DataSetResponse.from(entity);
    }
    /** Deletes the policy and its join rows while preserving all reusable rules. */
    @Transactional
    public void delete(UUID id) { repository.delete(require(id)); }

    private DataSet require(UUID id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Data Set"));
    }
    /** Checks all IDs before changing policy membership, preserving the old state on invalid updates. */
    private void apply(DataSet entity, DataSetRequest input) {
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
