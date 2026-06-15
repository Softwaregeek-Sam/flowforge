package dev.sumit.flowforge.registry;

import dev.sumit.flowforge.domain.Dag;
import dev.sumit.flowforge.domain.DagTask;
import dev.sumit.flowforge.domain.repositories.DagRepository;
import dev.sumit.flowforge.domain.repositories.DagTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DagRegistryService integration tests")
public class DagRegistryServiceTest {

    @Autowired
    private DagRegistryService registryService;

    @Autowired
    private DagRepository dagRepository;

    @Autowired
    private DagTaskRepository dagTaskRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private Path tempDir;

    private Path createTempDagDir() throws IOException {
        tempDir = Files.createTempDirectory("flowforge-test-dags");
        return tempDir;
    }

    private File writeDagYaml(Path dir, String fileName, String content) throws IOException {
        File file = dir.resolve(fileName).toFile();
        try(FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        return file;
    }

    @AfterEach
    void cleanUp() throws  IOException{
        jdbcTemplate.update("DELETE FROM dag_task_dependency", Map.of());
        jdbcTemplate.update("DELETE FROM dag_task", Map.of());
        jdbcTemplate.update("DELETE FROM dag_task", Map.of());

        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("should insert dag, task, and correct dependency when loading yaml")
    void should_insert_dag_with_tasks_and_dependencies() throws Exception{
        Path dir = createTempDagDir();

        writeDagYaml(dir, "backup.yml", """
        name: db_backup
        description: Daily backup
        schedule_cron: "0 2 * * *"
        is_active: true
        tasks:
          - name: check_disk
            type: SHELL
            config:
              command: "df -h /"
            retry_limit: 2
            retry_delay_seconds: 30
          - name: dump_db
            type: SHELL
            config:
              command: "pg_dump mydb"
            retry_limit: 3
            retry_delay_seconds: 60
            depends_on:
              - check_disk
        """);

        registryService.loadFromDirectory(dir.toString());


        Dag dag = dagRepository.findByName("db_backup")
                .orElseThrow(
                        () -> new AssertionError("DAG 'db_backup' was not inserted")
                );
        assertThat(dag.getScheduleCron()).isEqualTo("0 2 * * *");
        assertThat(dag.isActive()).isTrue();

        List<DagTask> tasks = dagTaskRepository.findByDagId(dag.getId());
        assertThat(tasks).hasSize(2);

        Map<String, Long> taskNameToId = tasks.stream()
                .collect(Collectors.toMap(
                        DagTask::getTaskName,
                        DagTask::getId
                ));


        assertThat(taskNameToId).containsKeys("check_disk", "dump_db");

        Long checkDiskId = taskNameToId.get("check_disk");
        Long dumpDbId = taskNameToId.get("dump_db");

        Integer depCount = jdbcTemplate.queryForObject("""
                 SELECT COUNT(*) FROM dag_task_dependency
                 WHERE dag_task_id = :dumbDbId
                 AND depends_on_task_id = :checkDiskId
                """,
                new MapSqlParameterSource()
                        .addValue("dumpDbId", dumpDbId)
                        .addValue("checkDiskId", checkDiskId),
                      Integer.class);


        assertThat(depCount)
                .as("dump_db must depends on check_disk, not reversed or self-referential")
                .isEqualTo(1);





    }

    void should_not_insert_duplicate_dag_on_second_load() throws Exception{
         Path dir = createTempDagDir();
        writeDagYaml(dir, "backup.yml", """
        name: db_backup
        is_active: true
        tasks:
          - name: check_disk
            type: SHELL
            config:
              command: "df -h /"
          - name: dump_db
            type: SHELL
            config:
              command: "pg_dump mydb"
            depends_on:
              - check_disk
        """);

        registryService.loadFromDirectory(dir.toString());

        Dag dagAfterFirstLoad = dagRepository.findByName("db_backup")
                .orElseThrow( () -> new AssertionError(
                        "First load failed -DAG 'db_backup' was not inserted. " +
                "Cannot test duplicate prevention without a successful first load"));

        List<DagTask> taskAfterFirstLoad = dagTaskRepository.findByDagId(dagAfterFirstLoad.getId());

        assertThat(taskAfterFirstLoad)
                .as("First load must insert exactly 2 task before second load runs")
                .hasSize(2);

        Integer depsAfterFirstLoad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dag_task_dependency",
                Map.of(),
                Integer.class
        );

        assertThat(depsAfterFirstLoad)
                .as("First load must exactly 1 dependency before second load runs")
                .isEqualTo(1);

        registryService.loadFromDirectory(dir.toString());

        List<Dag> allDags = dagRepository.findAll();
        assertThat(allDags)
                .as("Second load must not create a duplicate dag row")
                .hasSize(1);

        List<DagTask> taskAfterSecondLoad = dagTaskRepository.findByDagId(dagAfterFirstLoad.getId());

        assertThat(taskAfterSecondLoad)
                .as("Second load must not create duplicate task rows")
                .hasSize(2);

        assertThat(taskAfterSecondLoad)
                .extracting(DagTask::getTaskName)
                .containsExactlyInAnyOrder("check_dist", "dumb_db");

        Integer depsAfterSecondLoad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dag_task_dependency",
                Map.of(),
                Integer.class);

        assertThat(depsAfterSecondLoad)
                .as("Second load must not duplicate dependency rows")
                .isEqualTo(1);

    }
     @Test
     @DisplayName("should add new task when yaml is updated with additional task")
    void should_add_new_task_when_yaml_updated() throws Exception{
        Path dir  = createTempDagDir();
        writeDagYaml(dir, "backup.yml", """
            name: db_backup
            is_active: true
            tasks:
              - name: check_disk
                type: SHELL
                config:
                  command: "df -h /"
            """);
        registryService.loadFromDirectory(dir.toString());

        // Updated version
        writeDagYaml(dir, "backup.yml", """
        name: db_backup
        is_active: true
        tasks:
          - name: check_disk
            type: SHELL
            config:
              command: "df -h /"

          - name: dump_db
            type: SHELL
            config:
              command: "pg_dump mydb"
            depends_on:
              - check_disk
        """);

        registryService.loadFromDirectory(dir.toString());

        Dag dag = dagRepository.findByName("db_backup")
                .orElseThrow(() ->
                        new AssertionError("DAG 'db_backup' should exist")

                );

        List<DagTask> tasks = dagTaskRepository.findByDagId(dag.getId());
        assertThat(tasks).hasSize(2);

        assertThat(tasks)
                .extracting(DagTask::getTaskName)
                .containsExactlyInAnyOrder("check_disk", "dump_db");

        Map<String, Long> taskNameToId = tasks.stream()
                .collect(Collectors.toMap(
                        DagTask::getTaskName,
                        DagTask::getId
                ));

        Long checkDiskId = taskNameToId.get("check_dist");
        Long dumpDbId = taskNameToId.get("dump_db");

        Integer dependencyCount = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*) FROM dag_task_dependency
                 WHERE dag_task_id = :dumpDbId
                 AND depends_on_task_id = :checkDiskId
                """,
                new MapSqlParameterSource()
                        .addValue("dumpDbId", dumpDbId)
                        .addValue("checkDiskId", checkDiskId),
                Integer.class
        );

        assertThat(dependencyCount)
                .as("dump_db should depend on check_disk")
                .isEqualTo(1);


    }

    @Test
    @DisplayName("should continue loading other files when one file fails validation")
    void should_continue_loading_when_one_file_fails() throws Exception{
        Path dir = createTempDagDir();


        // Invalid YAML — has a cycle
        writeDagYaml(dir, "bad.yml", """
            name: cyclic_dag
            is_active: true
            tasks:
              - name: task_a
                type: SHELL
                config:
                  command: "echo a"
                depends_on:
                  - task_b
              - name: task_b
                type: SHELL
                config:
                  command: "echo b"
                depends_on:
                  - task_a
            """);

        // Valid YAML
        writeDagYaml(dir, "good.yml", """
            name: valid_dag
            is_active: true
            tasks:
              - name: step_one
                type: SHELL
                config:
                  command: "echo one"
            """);

        registryService.loadFromDirectory(dir.toString());

        assertThat(dagRepository.findByName("cyclid_dag")).isEmpty();

        assertThat(dagRepository.findByName("valid_dag")).isPresent();
    }


    @Test
    @DisplayName("should handle empty dags directory gracefully")
    void should_handle_empty_directory_gracefully() throws Exception {
        Path dir = createTempDagDir();
        // No YAML files

        // Should not throw
        registryService.loadFromDirectory(dir.toString());

        assertThat(dagRepository.findAll()).isEmpty();
    }



}
