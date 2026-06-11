package dev.sumit.flowforge.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "dag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
         createdAt = LocalDateTime.now();
         updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected  void onUpdate(){
         updatedAt = LocalDateTime.now();
    }
}