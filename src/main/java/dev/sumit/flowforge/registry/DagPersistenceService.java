package dev.sumit.flowforge.registry;


import dev.sumit.flowforge.domain.Dag;
import dev.sumit.flowforge.domain.DagTask;
import dev.sumit.flowforge.domain.repositories.DagRepository;
import dev.sumit.flowforge.domain.repositories.DagTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DagPersistenceService {

    private final DagRepository dagRepository;
    private final DagTaskRepository dagTaskRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public void upsert(DagDefinition definition) {
       Optional<Dag> existing =  dagRepository.findByName(definition.getName());

       if(existing.isEmpty()){
           insertNewDag(definition);
       }else{
           updateExistingDag(existing.get(), definition);
       }
    }



    private void insertNewDag(DagDefinition definition) {
        log.info("Inserting new DAG: '{}'", definition.getName());

        Dag dag = dagRepository.save(
                Dag.builder()
                        .name(definition.getName())
                        .description(definition.getDescription())
                        .scheduleCron(definition.getScheduleCron())
                        .isActive(definition.isActive())
                        .build()
        );

        Map<String, Long> taskNameToId = insertTasks(dag.getId(), definition.getTasks());
        insertAllDependencies(definition.getTasks(), taskNameToId);

        log.info("DAG '{}' inserted with {} task(s)",
                definition.getName(), definition.getTasks().size());
    }

    private void updateExistingDag(Dag existingDag, DagDefinition definition) {
        log.debug("DAG '{}' already exists - checking for new tasks", definition.getName());

        List<DagTask> existingTasks = dagTaskRepository.findByDagId(existingDag.getId());

       Set<String> existingTaskNames = new HashSet<>();
       Map<String, Long> taskNameToId = new HashMap<>();

       for(DagTask task : existingTasks){
        existingTaskNames.add(task.getTaskName());
        taskNameToId.put(task.getTaskName(), task.getId());
       }

       List<DagDefinition.TaskDefinition> newTaskDefs = definition.getTasks().stream()
            .filter(t-> !existingTaskNames.contains(t.getName()))
            .toList();

       if(newTaskDefs.isEmpty()){
           log.debug("DAG  '{}' - no new tasks detected", definition.getName());
           return;
       }

       Map<String, Long> newTaskNameToId = insertTasks(existingDag.getId(), newTaskDefs);
       taskNameToId.putAll(newTaskNameToId);

       insertAllDependencies(newTaskDefs, taskNameToId);

       log.info("DAG '{}' updated - {} new task(s) added",
               definition.getName(), newTaskDefs.size());


    }





    private Map<String, Long> insertTasks(Long dagId, List<DagDefinition.TaskDefinition> tasks) {
        Map<String, Long> taskNameToId = new HashMap<>();

        for(DagDefinition.TaskDefinition task : tasks ){
            DagTask saved  = dagTaskRepository.save(DagTask.builder()
                            .dagId(dagId)
                            .taskName(task.getName())
                            .taskType(task.getType())
                            .taskConfig(task.getConfig())
                            .retryLimit(task.getRetryLimit())
                            .retryDelaySeconds(task.getRetryDelaySeconds())
                    .build());

            taskNameToId.put(saved.getTaskName(), saved.getId());
            log.debug("Inserted task '{}' id={}", saved.getTaskName(), saved.getId());
        }

        return taskNameToId;
    }
    private void insertAllDependencies(List<DagDefinition.TaskDefinition> taskDefs, Map<String, Long> taskNameToId) {

        for(DagDefinition.TaskDefinition taskDef: taskDefs){
            if(taskDef.getDependsOn() == null || taskDef.getDependsOn().isEmpty()){
                continue;
            }

            Long taskId = taskNameToId.get(taskDef.getName());

            for (String dependsOnName : taskDef.getDependsOn()) {
                Long dependsOnId = taskNameToId.get(dependsOnName);
                if (dependsOnId != null) {
                    insertDependencyRow(taskId, dependsOnId);
                }
            }
        }

    }

    private void insertDependencyRow(Long taskId, Long dependsOnId) {
        jdbcTemplate.update("""
                INSERT INTO dag_task_dependency(dag_task_id, depends_on_task_id)
                VALUES (:taskId, :dependsOnId)
                ON CONFLICT DO NOTHING
                """,
                 new MapSqlParameterSource()
                         .addValue("taskId", taskId)
                         .addValue("dependsOnId", dependsOnId)

        );
    }
}
