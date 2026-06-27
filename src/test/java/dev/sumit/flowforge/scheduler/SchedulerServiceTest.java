package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.domain.Dag;
import dev.sumit.flowforge.domain.DagRun;
import dev.sumit.flowforge.domain.TaskRun;
import dev.sumit.flowforge.domain.enums.DagRunStatus;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 class SchedulerServiceTest {

    @Mock private DagLockManager      dagLockManager;
    @Mock private DagRunScheduler     dagRunScheduler;
    @Mock private TaskEligibilityChecker taskEligibilityChecker;
    @Mock private TaskRunCreator      taskRunCreator;
    @Mock private TaskDispatcher      taskDispatcher;
    @Mock private DagStateManager     dagStateManager;

    @InjectMocks SchedulerService schedulerService;


    private static final Instant NOW      = Instant.parse("2024-01-15T10:00:00Z");
    private static final String  CRON     = "0 * * * * *";
    private SchedulableDag dag;
    void setUp(){
         dag = new SchedulableDag(1L, "db_backup", CRON);
    }


    @Nested
    @DisplayName("Advisory Lock")
     class AdvisoryLockTests{

        @Test
        @DisplayName("skip all pipelines steps when lock cannot be acquired")
        void should_skp_processing_when_lock_cannot_be_acquired(){

            when(dagLockManager.tryAcquireLock(dag.dagId())).thenReturn(false);
            schedulerService.processDag(dag, NOW);

            verify(dagLockManager).tryAcquireLock(dag.dagId());

            verifyNoInteractions(
                    dagLockManager,
                    dagStateManager,
                    taskDispatcher
            );

            verify(dagLockManager, never()).releaseLock(anyLong());
        }

        @Test
        @DisplayName("release lock after successful pipeline execution")
        void should_release_lock_after_successful_processing(){
            lockAcquired();
            noNewDagRun();
            noPhaseTwoWork();

            schedulerService.processDag(dag, NOW);
            verify(dagLockManager).releaseLock(dag.dagId());

        }

        @Test
        @DisplayName("isolates pipeline failures and always releases the advisory lock")
        void should_isolate_pipeline_failures_and_release_lock() {

            lockAcquired();

            noNewDagRun();

            when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW))
                    .thenThrow(new RuntimeException("boom"));

            schedulerService.processDag(dag, NOW);

            verify(dagLockManager).releaseLock(dag.dagId());
        }


    }


    @Nested
    @DisplayName("Dag Run Scheduling")
    class DagRunSchedulingTests{

        @Test
        @DisplayName("attempt to schedule new dag run when lock is acquired and no new dag run is scheduled")
        void should_attempt_to_schedule_new_dag_run(){
            lockAcquired();
            noNewDagRun();
            noPhaseTwoWork();

            schedulerService.processDag(dag,NOW);
            verify(dagRunScheduler).scheduleIfDue(dag.dagId(), CRON, NOW);
        }

    }

    @Nested
    @DisplayName("Active Dag Run Progression")
    class ActiveDagRunProgression{

        @Test
        @DisplayName("progress existing active dag runs when no new dag run is created")
        void should_progress_existing_active_runs_when_no_new_run_is_created(){

            lockAcquired();
            noNewDagRun();

            when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW))
                    .thenReturn(List.of());

            schedulerService.processDag(dag,NOW);

            verify(dagStateManager).progressActiveDagRuns(dag.dagId(), NOW);
        }

        @Test
        @DisplayName("progress existing active dag runs after new dag run is created")
        void should_progress_existing_active_runs_after_new_run_creation() {
            lockAcquired();

            DagRun newDagRun =  newPendingDagRun();

            when(dagRunScheduler.scheduleIfDue(dag.dagId(), CRON, NOW))
                    .thenReturn(Optional.of(newDagRun));

            when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW))
                    .thenReturn(List.of());

            schedulerService.processDag(dag, NOW);

            verify(dagStateManager)
                    .progressActiveDagRuns(dag.dagId(), NOW);


        }
        @Test
        @DisplayName("dispatches task_runs created while progressing active dag_runs")
        void should_dispatch_created_task_runs() {

            lockAcquired();

            noNewDagRun();

            TaskRun t1 = taskRun();
            TaskRun t2 = taskRun();

            when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW))
                    .thenReturn(List.of(t1, t2));

            schedulerService.processDag(dag, NOW);

            verify(taskDispatcher)
                    .dispatch(List.of(t1, t2));
        }


        @Test
        @DisplayName("does not dispatch when no task_runs are created")
        void should_not_dispatch_when_no_task_runs_are_created() {

            lockAcquired();

            noNewDagRun();

            when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW))
                    .thenReturn(List.of());

            schedulerService.processDag(dag, NOW);

            verify(taskDispatcher, never())
                    .dispatch(any());
        }



    }




    @Nested
    @DisplayName("Transactional invariants")
    class TransactionalInvariantTests {

        @Test
        @DisplayName("dispatches task_runs only after active dag_runs have been progressed")
        void should_dispatch_after_progressing_active_runs() {

            lockAcquired();

            noNewDagRun();

            TaskRun taskRun = taskRun();

            when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW))
                    .thenReturn(List.of(taskRun));

            schedulerService.processDag(dag, NOW);

            InOrder order = inOrder(
                    dagStateManager,
                    taskDispatcher
            );

            order.verify(dagStateManager)
                    .progressActiveDagRuns(dag.dagId(), NOW);

            order.verify(taskDispatcher)
                    .dispatch(List.of(taskRun));
        }
    }


    private void lockAcquired() {
        when(dagLockManager.tryAcquireLock(dag.dagId())).thenReturn(true);
    }

    private void noNewDagRun() {
        when(dagRunScheduler.scheduleIfDue(dag.dagId(), CRON, NOW)).thenReturn(Optional.empty());
    }

    private void noPhaseTwoWork() {
        when(dagStateManager.progressActiveDagRuns(dag.dagId(), NOW)).thenReturn(List.of());

    }


    private DagRun newPendingDagRun() {

        return DagRun.createSchedulerRun(dag.dagId());
    }

    private TaskRun taskRun() {
        return TaskRun.create(1L, 1L);
    }




}
