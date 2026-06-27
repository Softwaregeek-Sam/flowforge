package dev.sumit.flowforge.scheduler;

import dev.sumit.flowforge.domain.repositories.DagRepository;
import dev.sumit.flowforge.scheduler.model.SchedulableDag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DagLoader {


    private final DagRepository dagRepository;
    public List<SchedulableDag> loadAllActive() {
       return dagRepository.findAllSchedulable();
    }

}
