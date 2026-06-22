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


    @Getter
    @Setter
    public static class Registry{
        @NotBlank(message = "flowforge.registry.dag-directory must not be blank")
        private String dagDirectory = "./dags";
    }
}
