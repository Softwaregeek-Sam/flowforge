package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.domain.TaskRun;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private DagLockManager dagLockManager;

    @Mock
    private SchedulingPipeline schedulingPipeline;

    @Mock
    private TaskDispatcher taskDispatcher;

    @InjectMocks
    private SchedulerService schedulerService;

    private static final Instant NOW =
            Instant.parse("2024-01-15T10:00:00Z");

    private static final String CRON = "0 * * * * *";

    private SchedulableDag dag;

    @BeforeEach
    void setUp() {
        dag = new SchedulableDag(
                1L,
                "db_backup",
                CRON
        );
    }


    @Nested
    @DisplayName("Advisory Lock")
    class AdvisoryLockTests {

        @Test
        @DisplayName("skips processing when advisory lock cannot be acquired")
        void should_skip_processing_when_lock_cannot_be_acquired() {

            when(dagLockManager.tryAcquireLock(dag.dagId()))
                    .thenReturn(false);

            schedulerService.processDag(dag, NOW);

            verify(dagLockManager)
                    .tryAcquireLock(dag.dagId());

            verifyNoInteractions(
                    schedulingPipeline,
                    taskDispatcher
            );

            verify(dagLockManager, never())
                    .releaseLock(anyLong());
        }

        @Test
        @DisplayName("releases advisory lock after successful processing")
        void should_release_lock_after_successful_processing() {

            lockAcquired();

            when(schedulingPipeline.execute(dag, NOW))
                    .thenReturn(List.of());

            schedulerService.processDag(dag, NOW);

            verify(dagLockManager)
                    .releaseLock(dag.dagId());
        }

        @Test
        @DisplayName("isolates pipeline failures and always releases the advisory lock")
        void should_isolate_pipeline_failures_and_release_lock() {

            lockAcquired();

            when(schedulingPipeline.execute(dag, NOW))
                    .thenThrow(new RuntimeException("Unexpected error while executing scheduling pipeline"));

            schedulerService.processDag(dag, NOW);

            verify(dagLockManager)
                    .releaseLock(dag.dagId());

            verify(taskDispatcher, never())
                    .dispatch(any());
        }
    }


    @Nested
    @DisplayName("Pipeline Delegation")
    class PipelineDelegationTests {

        @Test
        @DisplayName("delegates DAG processing to the scheduling pipeline")
        void should_delegate_processing_to_scheduling_pipeline() {

            lockAcquired();

            when(schedulingPipeline.execute(dag, NOW))
                    .thenReturn(List.of());

            schedulerService.processDag(dag, NOW);

            verify(schedulingPipeline)
                    .execute(dag, NOW);
        }
    }


    @Nested
    @DisplayName("Task Dispatch")
    class TaskDispatchTests {

        @Test
        @DisplayName("dispatches every task run returned by the scheduling pipeline")
        void should_dispatch_task_runs_returned_by_pipeline() {

            lockAcquired();

            TaskRun taskRun1 = taskRun();
            TaskRun taskRun2 = taskRun();

            when(schedulingPipeline.execute(dag, NOW))
                    .thenReturn(List.of(taskRun1, taskRun2));

            schedulerService.processDag(dag, NOW);

            verify(taskDispatcher)
                    .dispatch(List.of(taskRun1, taskRun2));
        }

        @Test
        @DisplayName("does not dispatch when scheduling pipeline returns no task runs")
        void should_not_dispatch_when_pipeline_returns_no_task_runs() {

            lockAcquired();

            when(schedulingPipeline.execute(dag, NOW))
                    .thenReturn(List.of());

            schedulerService.processDag(dag, NOW);

            verify(taskDispatcher, never())
                    .dispatch(any());
        }
    }


    @Nested
    @DisplayName("Execution Order")
    class ExecutionOrderTests {

        @Test
        @DisplayName("dispatches task runs only after pipeline execution completes")
        void should_dispatch_after_pipeline_execution() {

            lockAcquired();

            TaskRun taskRun = taskRun();

            when(schedulingPipeline.execute(dag, NOW))
                    .thenReturn(List.of(taskRun));

            schedulerService.processDag(dag, NOW);

            InOrder order = inOrder(
                    schedulingPipeline,
                    taskDispatcher
            );

            order.verify(schedulingPipeline)
                    .execute(dag, NOW);

            order.verify(taskDispatcher)
                    .dispatch(List.of(taskRun));
        }
    }


    private void lockAcquired() {
        when(dagLockManager.tryAcquireLock(dag.dagId()))
                .thenReturn(true);
    }

    private TaskRun taskRun() {
        return TaskRun.create(1L, 1L);
    }
}