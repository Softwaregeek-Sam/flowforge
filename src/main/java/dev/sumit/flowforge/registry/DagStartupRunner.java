package dev.sumit.flowforge.registry;

import dev.sumit.flowforge.infrastructure.FlowForgeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DagStartupRunner implements ApplicationRunner {

    private final DagRegistryService dagRegistryService;
    private final FlowForgeProperties flowForgeProperties;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        String dagDirectory = flowForgeProperties.getRegistry().getDagDirectory();
        dagRegistryService.loadDag(dagDirectory);
    }
}
