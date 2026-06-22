package dev.sumit.flowforge.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.nio.file.Path;

@Component
@Slf4j

public class DagYamlParser {

        private final ObjectMapper yamlMapper;

    public DagYamlParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());

        this.yamlMapper.setPropertyNamingStrategy(
                PropertyNamingStrategies.SNAKE_CASE
        );
    }


    public DagDefinition parse(Path filePath) {
        log.debug("Parsing DAG file: {}", filePath.getFileName());

        try{
            DagDefinition definition = yamlMapper.readValue(filePath.toFile(), DagDefinition.class);

            log.debug("Successfully parsed DAG '{}' from '{}'",
                    definition.getName(), filePath.getFileName());
            return definition;

        }catch(Exception e){
            throw new RuntimeException(
                    "Failed to parse DAG file '%s': %s"
                            .formatted(filePath.getFileName(), e.getMessage()), e
            );
        }
    }
}
