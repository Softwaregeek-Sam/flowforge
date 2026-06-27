package dev.sumit.flowforge.scheduler.model;

public record SchedulableDag(Long dagId,
                             String dagName,
                             String cronExpression) {

}
