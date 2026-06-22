package dev.sumit.flowforge.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class DagFileScanner {
    public List<Path> scan(String directory) {
        Path dirPath = Paths.get(directory);

       validateDirectory(dirPath);

        return listYamlFiles(dirPath);
    }


    private void validateDirectory(Path dirPath) {
        if(!Files.exists(dirPath)){
             throw new IllegalArgumentException("DAG directory " +dirPath +" not found  - no DAGs loaded" );

        }

        if(!Files.isDirectory(dirPath)){
            throw new IllegalArgumentException(
                    "Path is not a directory: " + dirPath
            );
        }
    }

    private List<Path> listYamlFiles(Path dirPath) {
        try(Stream<Path> files =  Files.list(dirPath)){
            List<Path> dagFiles = files
                    .filter(Files::isRegularFile)
                    .filter(this::isYamlFiles)
                    .sorted()
                    .toList();

            return  dagFiles;



        }catch (IOException e){
            throw new RuntimeException(
                    "Failed to scan DAG directory: " + dirPath
            );



        }
    }

    private boolean isYamlFiles(Path path){
        String fileName  = path.getFileName().toString();
        return fileName.endsWith(".yml")
                || fileName.endsWith(".yaml");
    }

}