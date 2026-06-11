package dev.sumit.flowforge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "dag_task")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="dag_id", nullable = false)
    private Long dagId;

  @Column(name = "task_name", nullable = false)
  private String taskName;
  @Column(name="task_type", nullable=false)
  private String taskType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name="task_config", columnDefinition = "jsonb")
    private Map<String, Object> taskConfig;

  private int retryLimit = 3;

  private int retryDelaySeconds = 60;
  private LocalDateTime createdAt;

  @PrePersist
    protected  void onCreate() {
      createdAt = LocalDateTime.now();
  }


}