package dev.sumit.flowforge.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DagValidator")
class DagValidatorTest {
    private DagValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DagValidator();
    }


    private DagDefinition.TaskDefinition task(String name, String... dependsOn) {
        DagDefinition.TaskDefinition t = new DagDefinition.TaskDefinition();
        t.setName(name);
        t.setType("SHELL");

        if (dependsOn.length > 0) {
            t.setDependsOn(List.of(dependsOn));
        }

        return t;
    }

    private DagDefinition dag(String name, DagDefinition.TaskDefinition... tasks) {
        DagDefinition d = new DagDefinition();
        d.setName(name);
        d.setTasks(List.of(tasks));

        return d;
    }

    @Nested
    @DisplayName("valid DAGs")
    class ValidDags {

        @Test
        @DisplayName("should pass when DAG has single task with no dependencies")
        void should_pass_when_single_task_no_dependencies() {
            DagDefinition d = dag("backup",
                    task("check_disk")
            );

            assertThatCode(() -> validator.validate(d))
                    .doesNotThrowAnyException();

        }

        @Test
        @DisplayName("should pass when DAG has linear chain of tasks")
        void should_pass_when_linear_chain() {
            DagDefinition d = dag("backup",
                    task("step_a"),
                    task("step_b", "step_a"),
                    task("step_c", "step_b")
            );

            assertThatCode(() -> validator.validate(d))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when DAG has parallel tasks joining to one task")
        void should_pass_when_parallel_tasks_join() {
            DagDefinition d = dag("pipeline",
                    task("fetch_data"),
                    task("validate_data"),
                    task("process_data", "fetch_data", "validate_data")
            );
            assertThatCode(() -> validator.validate(d))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when DAG has diamond dependency shape")
        void should_pass_when_diamond_dependency() {
            // A → B → D
            // A → C → D
            DagDefinition d = dag("diamond",
                    task("A"),
                    task("B", "A"),
                    task("C", "A"),
                    task("D", "B", "C")
            );
            assertThatCode(() -> validator.validate(d))
                    .doesNotThrowAnyException();
        }

    }

    @Nested
    @DisplayName("empty or null tasks")
    class EmptyTasks {

        @Test
        @DisplayName("should throw when DAG has no tasks")
        void should_throw_when_no_tasks() {
            DagDefinition d = new DagDefinition();
            d.setName("empty_dag");
            d.setTasks(List.of());

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty_dag");

        }

        @Test
        @DisplayName("should throw when tasks list is null")
        void should_throw_when_tasks_null() {
            DagDefinition d = new DagDefinition();
            d.setName("null_tasks_dag");
            d.setTasks(null);

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


    @Nested
    @DisplayName("duplicate task names")
    class DuplicateNames {

        @Test
        @DisplayName("should throw when two tasks have the same name")
        void should_throw_when_duplicate_task_name() {
            DagDefinition d = dag("backup",
                    task("step_a"),
                    task("step_a")   // duplicate
            );

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("backup")
                    .hasMessageContaining("step_a")
                    .hasMessageContaining("duplicate");
        }

        @Test
        @DisplayName("should throw when three tasks have duplicates among them")
        void should_throw_when_multiple_duplicates() {
            DagDefinition d = dag("backup",
                    task("step_a"),
                    task("step_b"),
                    task("step_a")   // duplicate
            );

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("step_a");
        }
    }


    @Nested
    @DisplayName("unknown dependency references")
    class UnknownDependencies {

        @Test
        @DisplayName("should throw when task depends on non-existent task")
        void should_throw_when_depends_on_unknown_task() {
            DagDefinition d = dag("backup",
                    task("dump_database", "chek_disk_space")  // typo — task doesn't exist
            );

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("backup")
                    .hasMessageContaining("dump_database")
                    .hasMessageContaining("chek_disk_space");
        }

        @Test
        @DisplayName("should throw when one of multiple dependencies is unknown")
        void should_throw_when_one_of_many_deps_unknown() {
            DagDefinition d = dag("pipeline",
                    task("step_a"),
                    task("step_b", "step_a", "step_nonexistent")
            );

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("step_nonexistent");
        }
    }

    // ── Cycle detection ────────────────────────────────────────────────────

    @Nested
    @DisplayName("cycle detection")
    class CycleDetection {

        @Test
        @DisplayName("should throw when two tasks depend on each other (simple cycle)")
        void should_throw_when_simple_cycle() {
            // A → B → A
            DagDefinition d = dag("cyclic",
                    task("A", "B"),
                    task("B", "A")
            );

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class);

        }

        @Test
        @DisplayName("should throw when three tasks form a cycle")
        void should_throw_when_three_task_cycle() {
            // A → B → C → A
            DagDefinition d = dag("cyclic",
                    task("A", "C"),
                    task("B", "A"),
                    task("C", "B")
            );

            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class);

        }

        @Test
        @DisplayName("should throw when task depends on itself")
        void should_throw_when_self_dependency() {
            DagDefinition d = dag("self_ref",
                    task("A", "A")   // self-dependency
            );

            // self-dependency also creates a cycle
            assertThatThrownBy(() -> validator.validate(d))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when cycle is deep in an otherwise valid graph")
        void should_throw_when_cycle_deep_in_graph() {

            DagDefinition cyclic = dag("deep_cycle",
                    task("A"),
                    task("B", "A"),
                    task("C", "E"),
                    task("D", "C"),
                    task("E", "D")
            );

            assertThatThrownBy(() -> validator.validate(cyclic))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


}
