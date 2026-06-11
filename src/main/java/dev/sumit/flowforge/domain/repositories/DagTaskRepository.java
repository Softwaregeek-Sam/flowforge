package dev.sumit.flowforge.domain.repositories;

import dev.sumit.flowforge.domain.DagTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface DagTaskRepository  extends JpaRepository<DagTask,Long> {
    List<DagTask> findByDagId(Long dagId);
}
