package dev.sumit.flowforge.registry;


import java.awt.*;
import java.util.*;
import java.util.List;

public class DagValidator {
    public void validate(DagDefinition dag) {
       validateTasksNotEmpty(dag);
       validateTaskNameUnique(dag);
       validateDependencyExist(dag);
       validateNoCycles(dag);
    }
    private void validateTasksNotEmpty(DagDefinition dag) {
        if(dag.getTasks() == null || dag.getTasks().isEmpty()) {
            throw new IllegalArgumentException(
                    "DAG '%s' has no tasks defined. A DAG must have at least one task"
                    .formatted(dag.getName()));
        }
    }
    private void validateTaskNameUnique(DagDefinition dag) {
        Set<String> seen = new HashSet<>();
        for(DagDefinition.TaskDefinition task : dag.getTasks()){
              if(!seen.add(task.getName())){
                  throw new IllegalArgumentException(
                          "DAG '%s' has a duplicate task name: '%s'."
                                  .formatted(dag.getName(), task.getName())
                  );
              }
        }
    }
    private void validateDependencyExist(DagDefinition dag) {
        Set<String> taskNames = new HashSet<>();

        for(DagDefinition.TaskDefinition task : dag.getTasks()){
            taskNames.add(task.getName());
        }

        for(DagDefinition.TaskDefinition task : dag.getTasks()){
             if(task.getDependsOn() == null) continue;

             for(String dep: task.getDependsOn()){
                 if(!taskNames.contains(dep)){
                     throw new IllegalArgumentException(
                             "DAG '%s' task '%s' depends on '%s' which does not exist"
                                     .formatted(dag.getName(), task.getName(), dep)
                     );
                 }
             }
        }

    }

    private void validateNoCycles(DagDefinition dag) {
        // Build adjacency map: taskName → list of tasks it depends on
        Map<String, List<String>> adjacency = new HashMap<>();
        for (DagDefinition.TaskDefinition task : dag.getTasks()) {
            adjacency.put(
                    task.getName(),
                    task.getDependsOn() != null ? task.getDependsOn() : List.of()
            );
        }

        // Color map — all start WHITE
        Map<String, Color> colors = new HashMap<>();
        for (String name : adjacency.keySet()) {
            colors.put(name, Color.WHITE);
        }

        // Run DFS from every unvisited node
        for (String taskName : adjacency.keySet()) {
            if (colors.get(taskName) == Color.WHITE) {
                dfs(taskName, adjacency, colors, dag.getName());
            }
        }
    }

    private void dfs(
            String current,
            Map<String, List<String>> adjacency,
            Map<String, Color> colors,
            String dagName) {

        // Mark as in-progress
        colors.put(current, Color.GRAY);

        for (String neighbor : adjacency.get(current)) {
            Color neighborColor = colors.get(neighbor);

            if (neighborColor == Color.GRAY) {
                // We reached a node that is currently in our call stack — cycle found
                throw new IllegalArgumentException(
                        "DAG '%s' has a cyclic dependency involving task '%s' and '%s'"
                                .formatted(dagName, current, neighbor)
                );
            }

            if (neighborColor == Color.WHITE) {
                dfs(neighbor, adjacency, colors, dagName);
            }
            // BLACK = already fully processed, safe to skip
        }

        // Mark as fully processed
        colors.put(current, Color.BLACK);
    }






}
