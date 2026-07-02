package dev.sumit.flowforge.scheduler;


import dev.sumit.flowforge.domain.DagRun;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingPipelineTest {

    @Mock
    private DagRunScheduler dagRunScheduler;

    @Mock
    private DagStateManager dagStateManager;

    @InjectMocks
    private SchedulingPipeline schedulingPipeline;

    private static final Instant NOW =
            Instant.parse("2024-01-15T10:00:00Z");

    private static final String CRON =
            "0 * * * * *";

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
    @DisplayName("Dag Run Scheduling")
    class DagRunSchedulingTests {

        @Test
        @DisplayName("attempts to schedule a new dag run")
        void should_attempt_to_schedule_new_dag_run() {


            when(dagStateManager.progressActiveDagRuns(
                    dag.dagId(),
                    NOW
            )).thenReturn(List.of());

            schedulingPipeline.execute(dag, NOW);

            verify(dagRunScheduler)
                    .scheduleIfDue(
                            dag.dagId(),
                            CRON,
                            NOW
                    );
        }
    }


    @Nested
    @DisplayName("Active Dag Run Progression")
    class ActiveDagRunProgressionTests {

        @Test
        @DisplayName("progresses active dag runs even when no new dag run is created")
        void should_progress_active_dag_runs_even_when_no_new_dag_run_is_created() {


            when(dagStateManager.progressActiveDagRuns(
                    dag.dagId(),
                    NOW
            )).thenReturn(List.of());

            schedulingPipeline.execute(dag, NOW);

            verify(dagStateManager)
                    .progressActiveDagRuns(
                            dag.dagId(),
                            NOW
                    );
        }

        @Test
        @DisplayName("returns task runs created while progressing active dag runs")
        void should_return_created_task_runs() {

            TaskRun taskRun1 = taskRun();
            TaskRun taskRun2 = taskRun();


            when(dagStateManager.progressActiveDagRuns(
                    dag.dagId(),
                    NOW
            )).thenReturn(List.of(taskRun1, taskRun2));

            List<TaskRun> result =
                    schedulingPipeline.execute(dag, NOW);

            assertThat(result)
                    .containsExactly(taskRun1, taskRun2);
        }
    }

    // -------------------------------------------------------------------------
    // Ordering
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Execution Order")
    class OrderingTests {

        @Test
        @DisplayName("attempts scheduling before progressing active dag runs")
        void should_schedule_before_progressing_active_dag_runs() {


            when(dagStateManager.progressActiveDagRuns(
                    dag.dagId(),
                    NOW
            )).thenReturn(List.of());

            schedulingPipeline.execute(dag, NOW);

            InOrder order = inOrder(
                    dagRunScheduler,
                    dagStateManager
            );

            order.verify(dagRunScheduler)
                    .scheduleIfDue(
                            dag.dagId(),
                            CRON,
                            NOW
                    );

            order.verify(dagStateManager)
                    .progressActiveDagRuns(
                            dag.dagId(),
                            NOW
                    );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DagRun newPendingDagRun() {
        return DagRun.createSchedulerRun(dag.dagId());
    }

    private TaskRun taskRun() {
        return TaskRun.create(1L, 1L);
    }
}