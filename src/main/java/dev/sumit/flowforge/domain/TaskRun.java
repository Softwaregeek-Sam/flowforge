package dev.sumit.flowforge.domain;

import dev.sumit.flowforge.domain.enums.TaskRunStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "task_run")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dag_run_id", nullable = false)
    private Long dagRunId;

    @Column(name = "dag_task_id", nullable = false)
    private Long dagTaskId;

    @Column(name = "task_run_id", nullable = false, unique = true, updatable = false)
    private UUID taskRunId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskRunStatus status;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private TaskRun(Long dagRunId, Long dagTaskId, int attemptNumber) {
        this.dagRunId = dagRunId;
        this.dagTaskId = dagTaskId;
        this.taskRunId = UUID.randomUUID();
        this.status = TaskRunStatus.PENDING;
        this.attemptNumber = attemptNumber;
        this.createdAt = LocalDateTime.now();
    }

    public static TaskRun create(Long dagRunId, Long dagTaskId) {
        return new TaskRun(dagRunId, dagTaskId, 1);
    }

    public static TaskRun retry(Long dagRunId, Long dagTaskId, int attemptNumber) {
        return new TaskRun(dagRunId, dagTaskId, attemptNumber);
    }

    public void assignWorker(String workerId) {
        this.workerId = workerId;
    }

    public void markRunning(LocalDateTime startedAt) {
        this.status = TaskRunStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public void markSucceeded(String resultJson, LocalDateTime finishedAt) {
        this.status = TaskRunStatus.SUCCESS;
        this.resultJson = resultJson;
        this.finishedAt = finishedAt;
    }

    public void markFailed(String errorMessage, LocalDateTime finishedAt) {
        this.status = TaskRunStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = finishedAt;
    }
}
