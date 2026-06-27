package dev.sumit.flowforge.scheduler;

import org.springframework.stereotype.Component;

@Component
public class DagLockManager {
    public boolean tryAcquireLock(Long aLong) {
        return false;
    }

    public void releaseLock(long l) {
    }
}
