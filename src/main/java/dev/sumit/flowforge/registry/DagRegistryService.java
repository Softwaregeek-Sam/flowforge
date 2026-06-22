package dev.sumit.flowforge.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DagRegistryService {

    private final DagFileScanner dagFileScanner;
    private final DagYamlParser yamlParser;
    private final DagValidator dagValidator;
    private final DagPersistenceService persistenceService;

    public void loadDag(String directory){
        log.info("DAG Registry starting — scanning: {}", directory);
            List<Path> dagFiles    =  dagFileScanner.scan(directory);

            if(dagFiles.isEmpty()){
               log.warn("NO DAGS found in {}", directory);
                return;
            }

            log.info("DAG Registry - found {} file(s) to process", dagFiles.size());

            int loaded  = 0;
            int failed = 0;

            for(Path file: dagFiles){
                 try{
                     loadFile(file);
                     loaded++;
                 }catch(Exception e){
                      log.error("DAG Registry - failed to load '{}' : {}",
                              file.getFileName(), e.getMessage());
                      failed++;
                 }
            }

            log.info("DAG Registry - complete. loaded={}, failed={}", loaded, failed);

    }

    private void loadFile(Path file){
        DagDefinition definition = yamlParser.parse(file);
        dagValidator.validate(definition);
        persistenceService.upsert(definition);

        log.info("DAG Registry - loaded '{}' from '{}'",
                definition.getName(), file.getFileName()
                );


    }
}
