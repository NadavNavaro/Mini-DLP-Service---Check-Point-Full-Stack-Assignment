package com.nadav.dlp.dataset;

import com.nadav.dlp.datatype.DataType;
import jakarta.persistence.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Groups shared rules into a policy without owning or cascading deletion to those rules. */
@Entity
@Table(name = "data_sets")
public class DataSet {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @ManyToMany
    @JoinTable(name = "data_set_types", joinColumns = @JoinColumn(name = "data_set_id"),
        inverseJoinColumns = @JoinColumn(name = "data_type_id"))
    private Set<DataType> dataTypes = new LinkedHashSet<>();

    protected DataSet() {}
    public DataSet(UUID id) { this.id = id; }
    public void update(String name, Collection<DataType> dataTypes) {
        this.name = name;
        this.dataTypes.clear();
        this.dataTypes.addAll(dataTypes);
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public Set<DataType> getDataTypes() { return Set.copyOf(dataTypes); }
}
