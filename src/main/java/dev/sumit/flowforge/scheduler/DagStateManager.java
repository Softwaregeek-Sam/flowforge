package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.domain.TaskRun;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DagStateManager {
    public List<TaskRun> progressActiveDagRuns(Long aLong, Instant now) {

        return List.of();
    }
}
