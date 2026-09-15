package com.campusiq.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.campusiq.repository.TestAttemptRepository;
import com.campusiq.service.MockTestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TestAttemptTimeoutScheduler {

    private static final int BATCH_SIZE = 100;

    private final TestAttemptRepository testAttemptRepository;
    private final MockTestService mockTestService;

    @Scheduled(
            initialDelayString = "${campusiq.test-timeout.initial-delay-ms:5000}",
            fixedDelayString = "${campusiq.test-timeout.fixed-delay-ms:5000}")
    public void finalizeExpiredAttempts() {
        LocalDateTime cutoff = LocalDateTime.now();
        long afterId = 0L;

        while (true) {
            List<Long> attemptIds;

            try {
                attemptIds = testAttemptRepository.findExpiredAttemptIds(
                        cutoff, afterId, PageRequest.of(0, BATCH_SIZE));
            } catch (Exception exception) {
                log.error("Unable to read expired test attempts; "
                        + "the next scheduled run will retry.", exception);
                return;
            }

            if (attemptIds.isEmpty()) {
                return;
            }

            for (Long attemptId : attemptIds) {
                try {
                    // Each service call uses its own transaction and attempt lock.
                    if (mockTestService.finalizeExpiredAttempt(attemptId)) {
                        log.info("Automatically finalized expired test attempt {}",
                                attemptId);
                    }
                } catch (Exception exception) {
                    log.error("Unable to finalize test attempt {}; "
                            + "the next scheduled run will retry.",
                            attemptId, exception);
                }

                // Continue past failures; retry them in the next scheduled run.
                afterId = attemptId;
            }
        }
    }
}