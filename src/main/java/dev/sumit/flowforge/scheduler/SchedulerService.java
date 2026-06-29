package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.domain.TaskRun;
import dev.sumit.flowforge.registry.DagStartupRunner;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final DagLockManager dagLockManager;
    private final TaskDispatcher taskDispatcher;
    private final SchedulingPipeline schedulingPipeline;


    public void processDag(SchedulableDag dag, Instant now) {
         boolean lockAcquired = dagLockManager.tryAcquireLock(dag.dagId());

         if(!lockAcquired) {
             log.debug("DAG [{}] skipped — could not acquire advisory lock (another instance is processing)", dag.dagId());
             return;
         }

         try{
             List<TaskRun> toDispatch = schedulingPipeline.execute(dag, now);

             if(!toDispatch.isEmpty()){
                 taskDispatcher.dispatch(toDispatch);
             }
         }catch(Exception e){
             log.error("Scheduling pipeline failed for DAG [{}]: {}",
                     dag.dagId(), e.getMessage(), e);

         }finally{
             dagLockManager.releaseLock(dag.dagId());
         }


    }


}
