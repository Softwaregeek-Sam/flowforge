package dev.sumit.flowforge.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "heartbeat")

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Heartbeat {
    @Id
    @Column(name = "task_run_id")
    private Long taskRunId;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    @PreUpdate
    protected void onTouch(){
     lastSeenAt = LocalDateTime.now();
    }
}