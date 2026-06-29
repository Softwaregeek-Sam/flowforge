package dev.sumit.flowforge.domain;

import dev.sumit.flowforge.domain.enums.DagRunStatus;
import dev.sumit.flowforge.domain.enums.TriggerType;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Entity
@Table(name = "dag_run")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DagRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dag_id", nullable = false)
    private Long dagId;

    @Column(name = "run_id", nullable = false, unique = true, updatable = false)
    private UUID runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DagRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, updatable = false)
    private TriggerType triggeredBy;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    private DagRun(Long dagId, TriggerType triggeredBy) {
        this.dagId = dagId;
        this.runId = UUID.randomUUID();
        this.status = DagRunStatus.PENDING;
        this.triggeredBy = triggeredBy;
        this.triggeredAt = LocalDateTime.now();
    }

    public static DagRun createSchedulerRun(Long dagId) {
        return new DagRun(dagId, TriggerType.SCHEDULER);
    }

    public static DagRun createManualRun(Long dagId) {
        return new DagRun(dagId, TriggerType.SCHEDULER);
    }

    public void markRunning(LocalDateTime startedAt) {
        this.status = DagRunStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public void markSucceeded(LocalDateTime finishedAt) {
        this.status = DagRunStatus.SUCCESS;
        this.finishedAt = finishedAt;
    }

    public void markFailed(LocalDateTime finishedAt) {
        this.status = DagRunStatus.FAILED;
        this.finishedAt = finishedAt;
    }
}