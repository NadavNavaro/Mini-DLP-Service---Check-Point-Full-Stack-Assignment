package com.nadav.dlp.datatype;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persists a reusable keyword rule; keyword order is retained for stable API responses. */
@Entity
@Table(name = "data_types")
public class DataType {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(nullable = false, length = 20)
    private String type;
    @ElementCollection
    @CollectionTable(name = "data_type_keywords", joinColumns = @JoinColumn(name = "data_type_id"))
    @OrderColumn(name = "keyword_order")
    @Column(name = "keyword", nullable = false, length = 100)
    private List<String> content = new ArrayList<>();
    @Column(nullable = false)
    private int threshold;

    protected DataType() {}

    public DataType(UUID id) { this.id = id; }

    public void update(String name, String description, List<String> content, int threshold) {
        this.name = name;
        this.description = description;
        this.type = "keywords";
        this.content.clear();
        this.content.addAll(content);
        this.threshold = threshold;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public List<String> getContent() { return List.copyOf(content); }
    public int getThreshold() { return threshold; }
}
