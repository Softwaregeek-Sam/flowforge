package dev.sumit.flowforge.domain.repositories;

import dev.sumit.flowforge.domain.DagRun;
import dev.sumit.flowforge.domain.enums.DagRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository

public interface DagRunRepository extends JpaRepository<DagRun, Long> {

    Optional<DagRun> findByDagIdAndStatus(Long dagId, DagRunStatus status);

    Optional<DagRun> findByRunId(UUID runId);

    List<DagRun> findByDagIdOrderByTriggeredAtDesc(Long dagId);


    Optional<DagRun> findFirstByDagIdAndStatusIn(Long dagId, Set<DagRunStatus> statuses);
}
