create table configs
(
    id           integer
        primary key,
    config_key   varchar(255) not null
        unique,
    config_value TEXT
);

INSERT INTO configs (id, config_key, config_value) VALUES
(1, 'HTTP_CONNECT_TIMEOUT', '10'),
(2, 'HTTP_READ_TIMEOUT', '30'),
(3, 'HTTP_WRITE_TIMEOUT', '30'),
(4, 'HTTP_MAX_POOL_SIZE', '100'),
(5, 'HTTP_KEEP_ALIVE_DURATION', '5');

create table projects
(
    created_at     timestamp,
    environment_id bigint       not null,
    id             integer
        primary key,
    updated_at     timestamp,
    description    TEXT,
    name           varchar(255) not null
);

create table environments
(
    created_at timestamp,
    id         integer
        primary key,
    updated_at timestamp
);

create table environment_variables
(
    is_active      boolean      not null,
    created_at     timestamp,
    environment_id bigint       not null,
    id             integer
        primary key,
    updated_at     timestamp,
    key            varchar(255) not null,
    value          TEXT
);

create table endpoints
(
    created_at             timestamp,
    id                     integer
        primary key,
    project_id             bigint       not null,
    updated_at             timestamp,
    description            TEXT,
    graphql_operation_type VARCHAR(20),
    graphql_variables      TEXT,
    grpc_method_name       varchar(255),
    grpc_service_name      varchar(255),
    http_body              TEXT,
    http_headers           TEXT,
    http_method            VARCHAR(10),
    http_parameters        TEXT,
    name                   varchar(255) not null,
    type                   VARCHAR(20)  not null,
    url                    varchar(255),
    grpc_stub_path         TEXT
);

create table flows
(
    created_at  timestamp,
    id          integer
        primary key,
    project_id  bigint       not null,
    updated_at  timestamp,
    description TEXT,
    name        varchar(255) not null
);

create table flow_steps
(
    created_at     timestamp,
    endpoint_id    bigint,
    flow_id        bigint      not null,
    updated_at     timestamp,
    condition      TEXT,
    id             VARCHAR(36) not null
        primary key,
    next_if_false  varchar(255),
    next_if_true   varchar(255),
    post_processor TEXT,
    pre_processor  TEXT,
    type           VARCHAR(10) not null
);

create table runs
(
    duration         integer     not null,
    ramp_up_duration integer     not null,
    threads          integer     not null,
    created_at       timestamp,
    flow_id          bigint      not null,
    id               integer
        primary key,
    updated_at       timestamp,
    status           VARCHAR(10) not null
);

create table request_logs
(
    status_code   integer not null,
    created_at    timestamp,
    endpoint_id   bigint  not null,
    id            integer
        primary key,
    response_time bigint  not null,
    run_id        bigint  not null,
    updated_at    timestamp,
    request       TEXT,
    response      TEXT
);





