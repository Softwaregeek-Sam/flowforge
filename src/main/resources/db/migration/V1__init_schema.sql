-- ============================================================
-- FlowForge initial schema
-- Version: V1
-- Description: Core tables for DAG orchestration
-- ============================================================

-- STATUS ENUMS
-- Using PostgreSQL native ENUM types enforces valid values
-- at the database level, not just application level.

CREATE TYPE dag_run_status AS ENUM (
    'PENDING',
    'RUNNING',
    'SUCCESS',
    'FAILED'
);

CREATE TYPE task_run_status AS ENUM (
    'PENDING',
    'RUNNING',
    'SUCCESS',
    'FAILED'
);

-- ============================================================
-- DEFINITION TABLES
-- These describe the blueprint. Written rarely, read often.
-- ============================================================

CREATE TABLE dag (
                     id            BIGSERIAL PRIMARY KEY,
                     name          VARCHAR(255) NOT NULL,
                     description   TEXT,
                     schedule_cron VARCHAR(100),
                     is_active     BOOLEAN NOT NULL DEFAULT true,
                     created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                     updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

                     CONSTRAINT uq_dag_name UNIQUE (name)
);

COMMENT ON TABLE  dag              IS 'Workflow definitions. One row per DAG.';
COMMENT ON COLUMN dag.schedule_cron IS 'Cron expression. NULL = manual trigger only.';
COMMENT ON COLUMN dag.is_active    IS 'Soft delete. Never hard delete a DAG.';

-- -------------------------------------------------------

CREATE TABLE dag_task (
                          id                  BIGSERIAL PRIMARY KEY,
                          dag_id              BIGINT NOT NULL,
                          task_name           VARCHAR(255) NOT NULL,
                          task_type           VARCHAR(100) NOT NULL,  -- e.g. SHELL, HTTP, SQL
                          task_config         JSONB,                  -- task-type-specific config
                          retry_limit         INT NOT NULL DEFAULT 3,
                          retry_delay_seconds INT NOT NULL DEFAULT 60,
                          created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_dag_task_dag
                              FOREIGN KEY (dag_id) REFERENCES dag(id),

                          CONSTRAINT uq_dag_task_name
                              UNIQUE (dag_id, task_name)              -- unique within a DAG, not globally
);

COMMENT ON TABLE  dag_task             IS 'Task definitions within a DAG.';
COMMENT ON COLUMN dag_task.task_type   IS 'Determines which executor the worker uses.';
COMMENT ON COLUMN dag_task.task_config IS 'JSONB config specific to task_type.';

-- -------------------------------------------------------
-- Task dependencies: dag_task → dag_task (self-referential)
-- One row per dependency edge.
-- Example: Task B depends on Task A → (B.id, A.id)

CREATE TABLE dag_task_dependency (
                                     dag_task_id        BIGINT NOT NULL,
                                     depends_on_task_id BIGINT NOT NULL,

                                     CONSTRAINT pk_dag_task_dependency
                                         PRIMARY KEY (dag_task_id, depends_on_task_id),

                                     CONSTRAINT fk_dependency_task
                                         FOREIGN KEY (dag_task_id)
                                             REFERENCES dag_task(id),

                                     CONSTRAINT fk_dependency_depends_on
                                         FOREIGN KEY (depends_on_task_id)
                                             REFERENCES dag_task(id),

    -- A task cannot depend on itself
                                     CONSTRAINT chk_no_self_dependency
                                         CHECK (dag_task_id != depends_on_task_id)
    );

COMMENT ON TABLE dag_task_dependency IS
    'Dependency edges between tasks. dag_task_id cannot run until depends_on_task_id is SUCCESS.';

-- ============================================================
-- INSTANCE TABLES
-- Created every time work happens. Write-heavy. Grow continuously.
-- ============================================================

CREATE TABLE dag_run (
                         id           BIGSERIAL PRIMARY KEY,
                         dag_id       BIGINT NOT NULL,
                         run_id       UUID NOT NULL DEFAULT gen_random_uuid(),
                         status       dag_run_status NOT NULL DEFAULT 'PENDING',
                         triggered_by VARCHAR(50) NOT NULL DEFAULT 'SCHEDULER', -- SCHEDULER | MANUAL
                         triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         started_at   TIMESTAMP,      -- set when first task transitions to RUNNING
                         finished_at  TIMESTAMP,      -- set when dag_run reaches terminal state

                         CONSTRAINT fk_dag_run_dag
                             FOREIGN KEY (dag_id) REFERENCES dag(id),

                         CONSTRAINT uq_dag_run_id
                             UNIQUE (run_id)
);

COMMENT ON TABLE  dag_run             IS 'One row per DAG execution instance.';
COMMENT ON COLUMN dag_run.run_id      IS 'External identifier. Exposed in API responses.';
COMMENT ON COLUMN dag_run.triggered_by IS 'SCHEDULER for cron runs. MANUAL for API-triggered.';

-- -------------------------------------------------------

CREATE TABLE task_run (
                          id             BIGSERIAL PRIMARY KEY,
                          dag_run_id     BIGINT NOT NULL,
                          dag_task_id    BIGINT NOT NULL,
                          task_run_id    UUID NOT NULL DEFAULT gen_random_uuid(),
                          attempt_number INT NOT NULL DEFAULT 1,
                          status         task_run_status NOT NULL DEFAULT 'PENDING',
                          worker_id      VARCHAR(255),   -- which worker handled this attempt
                          started_at     TIMESTAMP,
                          finished_at    TIMESTAMP,
                          result_json    JSONB,          -- task output, structure varies by task_type
                          error_message  TEXT,           -- full error, TEXT not VARCHAR (stack traces are long)
                          created_at     TIMESTAMP NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_task_run_dag_run
                              FOREIGN KEY (dag_run_id) REFERENCES dag_run(id),

                          CONSTRAINT fk_task_run_dag_task
                              FOREIGN KEY (dag_task_id) REFERENCES dag_task(id),

                          CONSTRAINT uq_task_run_id
                              UNIQUE (task_run_id),

    -- Attempt numbers must be positive
                          CONSTRAINT chk_attempt_positive
                              CHECK (attempt_number > 0)
);

COMMENT ON TABLE  task_run                IS 'One row per task execution attempt. Never updated for history — new row per retry.';
COMMENT ON COLUMN task_run.task_run_id    IS 'Idempotency key. Stable across queue redeliveries.';
COMMENT ON COLUMN task_run.attempt_number IS 'Starts at 1. Increments per retry.';
COMMENT ON COLUMN task_run.worker_id      IS 'Set when worker picks up the task.';

-- -------------------------------------------------------
-- One row per running task. Upserted every 15 seconds.
-- Monitor queries: WHERE last_seen_at < NOW() - INTERVAL '60 seconds'

CREATE TABLE heartbeat (
                           task_run_id  BIGINT PRIMARY KEY,       -- one row per task_run, not per beat
                           worker_id    VARCHAR(255) NOT NULL,
                           last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),

                           CONSTRAINT fk_heartbeat_task_run
                               FOREIGN KEY (task_run_id) REFERENCES task_run(id)
);

COMMENT ON TABLE heartbeat IS
    'Worker liveness signal. Upserted every 15s per running task. Monitor detects death via last_seen_at.';

-- ============================================================
-- INDEXES
-- Driven by access patterns, not intuition.
-- ============================================================

-- Scheduler: find active DAGs
CREATE INDEX idx_dag_is_active
    ON dag (is_active);

-- Scheduler: find existing runs for a DAG by status
CREATE INDEX idx_dag_run_dag_id_status
    ON dag_run (dag_id, status);

-- Scheduler + Monitor: find task runs by run and status
CREATE INDEX idx_task_run_dag_run_status
    ON task_run (dag_run_id, status);

-- Monitor: find all RUNNING tasks efficiently
CREATE INDEX idx_task_run_status
    ON task_run (status);

-- Monitor: find stale heartbeats
CREATE INDEX idx_heartbeat_last_seen_at
    ON heartbeat (last_seen_at);

-- API: look up a dag_run by its external UUID
CREATE INDEX idx_dag_run_run_id
    ON dag_run (run_id);

-- API: look up a task_run by its external UUID
CREATE INDEX idx_task_run_task_run_id
    ON task_run (task_run_id);