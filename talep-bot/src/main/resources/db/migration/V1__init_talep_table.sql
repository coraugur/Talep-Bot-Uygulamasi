CREATE TABLE talep (
    id              VARCHAR(36)     PRIMARY KEY,
    description     VARCHAR(2000)   NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    user_story      TEXT,
    tech_spec       TEXT,
    code_output     TEXT,
    test_report     TEXT,
    deploy_report   TEXT,
    error_message   TEXT,
    iteration_count INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_talep_status ON talep(status);
CREATE INDEX idx_talep_created_at ON talep(created_at DESC);
