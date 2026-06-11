package dev.sumit.flowforge.registry;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DagDefinition {
    private String name;

    private String description;

    private String scheduleCron;

    private boolean isActive = true;

    private List<TaskDefinition> tasks;

    @Data
    public static class TaskDefinition{
         private String name;
         private String type;

         private Map<String, Object> config;

         private int retryLimit = 3;

         private int retryDelaySeconds = 60;

         private List<String > dependsOn;


    }
}
