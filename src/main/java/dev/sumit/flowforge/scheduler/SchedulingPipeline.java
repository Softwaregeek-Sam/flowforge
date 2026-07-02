package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.domain.TaskRun;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingPipeline {

    private final DagRunScheduler dagRunScheduler;

    private final DagStateManager dagStateManager;

    @Transactional
    public List<TaskRun> execute(SchedulableDag dag, Instant now) {

        dagRunScheduler.scheduleIfDue(dag.dagId(), dag.cronExpression(), now);

        return dagStateManager.progressActiveDagRuns(dag.dagId(), now);
    }
}
