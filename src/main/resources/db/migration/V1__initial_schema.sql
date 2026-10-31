CREATE TABLE data_types (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type = 'keywords'),
    threshold INTEGER NOT NULL CHECK (threshold > 0)
);

CREATE TABLE data_type_keywords (
    data_type_id UUID NOT NULL REFERENCES data_types(id) ON DELETE CASCADE,
    keyword_order INTEGER NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    PRIMARY KEY (data_type_id, keyword_order)
);

CREATE TABLE data_sets (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE data_set_types (
    data_set_id UUID NOT NULL REFERENCES data_sets(id) ON DELETE CASCADE,
    data_type_id UUID NOT NULL REFERENCES data_types(id) ON DELETE RESTRICT,
    PRIMARY KEY (data_set_id, data_type_id)
);
CREATE INDEX idx_data_set_types_type ON data_set_types(data_type_id);
