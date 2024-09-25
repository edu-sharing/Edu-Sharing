CREATE TABLE edu_timed_node_permission
(
    node_id    varchar(36) not null,
    authority  varchar(100) not null,
    permission varchar(100) not null,
    "from"     timestamp,
    "to"       timestamp,
    "activated" boolean not null,
    CONSTRAINT chk_from_to_null CHECK (("from" IS NOT NULL) OR ("to" IS NOT NULL)),
    CONSTRAINT chk_time CHECK ("from" < "to")
);

CREATE INDEX idx_node_id ON edu_timed_node_permission (node_id);
CREATE INDEX idx_from ON edu_timed_node_permission ("from");
CREATE INDEX idx_to ON edu_timed_node_permission ("to");
CREATE UNIQUE INDEX id_node_id_authority_permission ON edu_timed_node_permission (node_id, authority, permission);

