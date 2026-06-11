package dev.sumit.flowforge.domain.repositories;

import dev.sumit.flowforge.domain.Dag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DagRepository extends JpaRepository<Dag, Long> {
    List<Dag> findByIsActiveTrue();
    Optional<Dag> findByName(String name);
}
