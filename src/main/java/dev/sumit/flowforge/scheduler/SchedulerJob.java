package dev.sumit.flowforge.scheduler;


import dev.sumit.flowforge.infrastructure.FlowForgeProperties;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerJob {

    private final DagLoader dagLoader;
    private final SchedulerService schedulerService;



    @Scheduled(fixedDelayString = "#{@flowForgeProperties.scheduler.intervalSeconds * 1000}")
    public void tick() {
        Instant now =  Instant.now();

        List<SchedulableDag> dags  = dagLoader.loadAllActive();
        log.debug("Scheduler tick at [{}] - processing {} DAG(s)", now, dags.size());


        for(SchedulableDag dag : dags){
             try{
                 schedulerService.processDag(
                       dag, now
                 );
             }catch (Exception e){
                  log.error(
                          "Failed to process DAG [{}]. Continuing with remaining DAGs.",
                          dag.dagName(), e
                  );
             }
        }


        log.debug("Scheduler tick at [{}] - completed processing {} DAG(s)", now, dags.size());

    }
}
