package dev.sumit.flowforge.domain;

import dev.sumit.flowforge.domain.enums.DagRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dag_run")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="dag_id", nullable = false)
    private Long dagId;

    @Column(name = "run_id", nullable = false, unique = true)
    private UUID runId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DagRunStatus status = DagRunStatus.PENDING;

    @Column(name= "triggered_by", nullable = false)
    private String triggeredBy = "scheduler";


    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate(){
         if(runId == null){
              runId = UUID.randomUUID();
              triggeredAt = LocalDateTime.now();
         }
    }

}