package dev.sumit.flowforge.domain.repositories;

import dev.sumit.flowforge.domain.Heartbeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HeartbeatRepository extends JpaRepository<Heartbeat, Long> {

    List<Heartbeat> findByLastSeenAtBefore(LocalDateTime threshold);
}
