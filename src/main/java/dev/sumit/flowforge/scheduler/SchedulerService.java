package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final DagLockManager dagLockManager;


    public void processDag(SchedulableDag dag, Instant now) {
         boolean lockAcquired = dagLockManager.tryAcquireLock(dag.dagId());

         if(!lockAcquired) {
             log.debug("DAG [{}] skipped — could not acquire advisory lock (another instance is processing)", dag.dagId());
             return;
         }


    }
}
