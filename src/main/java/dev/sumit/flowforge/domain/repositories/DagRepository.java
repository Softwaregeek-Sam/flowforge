package dev.sumit.flowforge.domain.repositories;

import dev.sumit.flowforge.domain.Dag;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DagRepository extends JpaRepository<Dag, Long> {
    List<Dag> findByIsActiveTrue();
    Optional<Dag> findByName(String name);


    @Query("""
        SELECT new dev.sumit.flowforge.scheduler.model.SchedulableDag(
            d.id,
            d.name,
            d.scheduleCron
        )
        FROM Dag d
        WHERE d.isActive = true
          AND d.scheduleCron IS NOT NULL
        ORDER BY d.id
    """)
    List<SchedulableDag> findAllSchedulable();


}
