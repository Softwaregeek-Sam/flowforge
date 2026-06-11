package dev.sumit.flowforge.registry;

import dev.sumit.flowforge.domain.repositories.DagRepository;
import dev.sumit.flowforge.domain.repositories.DagTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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




}
