package dev.sumit.flowforge.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "flowforge")
public class FlowForgeProperties {

    @NotNull
    private Registry registry = new Registry();

    @NotNull
    private Scheduler scheduler = new Scheduler();

    @NotNull
    private Monitor monitor = new Monitor();

    @NotNull
    private Worker worker = new Worker();


    @Getter
    @Setter
    public static class Registry {
        @NotBlank(message = "flowforge.registry.dag-directory must not be blank")
        private String dagDirectory = "./dags";
    }

    @Getter
    @Setter
    public static class Scheduler {
        @NotNull(message = "flowforge.scheduler.interval-seconds must not be null")
        private Integer intervalSeconds = 30;
    }

    @Getter
    @Setter
    public static class Monitor {
        @NotNull(message = "flowforge.monitor.heartbeat-timeout-seconds must not be null")
        private Integer heartbeatTimeoutSeconds = 60;

        @NotNull(message = "flowforge.monitor.check-interval-seconds must not be null")
        private Integer checkIntervalSeconds = 10;
    }

    @Getter
    @Setter
    public static class Worker {
        @NotNull(message = "flowforge.worker.heartbeat-interval-seconds must not be null")
        private Integer heartbeatIntervalSeconds = 15;

        @NotBlank(message = "flowforge.worker.queue-key must not be blank")
        private String queueKey = "flowforge:tasks";
    }
}
