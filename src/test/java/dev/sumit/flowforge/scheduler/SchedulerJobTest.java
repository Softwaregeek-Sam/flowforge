package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerJobTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private DagLoader dagLoader;

    @InjectMocks
    private SchedulerJob schedulerJob;

    private Long dagId1;
    private Long dagId2;

    @BeforeEach
    void setUp() {
        dagId1 = 1L;
        dagId2 = 2L;
    }

    @Test
    @DisplayName("processes every active DAG loaded from the registry")
    void should_process_every_active_dag() {

        // Arrange
        SchedulableDag dag1 =
                new SchedulableDag(dagId1, "db_backup", "0 * * * * *");

        SchedulableDag dag2 =
                new SchedulableDag(dagId2, "daily_backup", "0 0 * * * *");

        when(dagLoader.loadAllActive())
                .thenReturn(List.of(dag1, dag2));

        // Act
        schedulerJob.tick();

        // Assert
        verify(dagLoader).loadAllActive();

        verify(schedulerService).processDag(eq(dag1), any(Instant.class));
        verify(schedulerService).processDag(eq(dag2), any(Instant.class));

        verifyNoMoreInteractions(dagLoader, schedulerService);
    }

    @Test
    @DisplayName("uses the same Instant for all DAGs within a scheduler tick")
    void should_use_same_now_instant_for_all_dags_in_one_tick() {

        // Arrange
        SchedulableDag dag1 =
                new SchedulableDag(dagId1, "db_backup", "0 * * * * *");

        SchedulableDag dag2 =
                new SchedulableDag(dagId2, "daily_backup", "0 0 * * * *");

        when(dagLoader.loadAllActive())
                .thenReturn(List.of(dag1, dag2));

        // Act
        schedulerJob.tick();

        // Assert
        ArgumentCaptor<Instant> instantCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(schedulerService, times(2))
                .processDag(any(SchedulableDag.class), instantCaptor.capture());

        List<Instant> instants = instantCaptor.getAllValues();

        assertThat(instants).hasSize(2);
        assertThat(instants.get(0)).isEqualTo(instants.get(1));

        verify(dagLoader).loadAllActive();
        verifyNoMoreInteractions(dagLoader, schedulerService);
    }

    @Test
    @DisplayName("does not process any DAG when no active DAGs exist")
    void should_not_process_any_dag_when_no_active_dags_exist() {

        // Arrange
        when(dagLoader.loadAllActive())
                .thenReturn(List.of());

        // Act
        schedulerJob.tick();

        // Assert
        verify(dagLoader).loadAllActive();
        verifyNoInteractions(schedulerService);
        verifyNoMoreInteractions(dagLoader);
    }

    @Test
    @DisplayName("continues processing remaining DAGs when one DAG fails")
    void should_continue_processing_remaining_dags_when_one_processing_fails() {

        // Arrange
        SchedulableDag dag1 =
                new SchedulableDag(dagId1, "db_backup", "0 * * * * *");

        SchedulableDag dag2 =
                new SchedulableDag(dagId2, "daily_backup", "0 0 * * * *");

        when(dagLoader.loadAllActive())
                .thenReturn(List.of(dag1, dag2));

        doThrow(new RuntimeException("boom"))
                .when(schedulerService)
                .processDag(eq(dag1), any(Instant.class));

        // Act
        schedulerJob.tick();

        // Assert
        verify(dagLoader).loadAllActive();

        verify(schedulerService).processDag(eq(dag1), any(Instant.class));
        verify(schedulerService).processDag(eq(dag2), any(Instant.class));

        verifyNoMoreInteractions(dagLoader, schedulerService);
    }
}