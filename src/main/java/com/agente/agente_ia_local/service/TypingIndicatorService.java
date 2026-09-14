package com.agente.agente_ia_local.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TypingIndicatorService {

    private static final Logger log = LoggerFactory.getLogger(TypingIndicatorService.class);

    @Value("${chat.typing.words-per-second:8}")
    private int wordsPerSecond;

    @Value("${chat.typing.max-delay-seconds:6}")
    private int maxDelaySeconds;

    @Value("${chat.typing.enabled:true}")
    private boolean enabled;

    public long calculateTypingDelay(String responseText) {
        if (!enabled || responseText == null || responseText.isBlank()) {
            return 0;
        }
        int wordCount = responseText.split("\\s+").length;
        long delayMs = (long) ((double) wordCount / wordsPerSecond * 1000);
        long maxDelayMs = (long) maxDelaySeconds * 1000;
        return Math.min(delayMs, maxDelayMs);
    }

    public void simulateTyping(Runnable onComplete, String responseText) {
        long delay = calculateTypingDelay(responseText);
        if (delay <= 0) {
            onComplete.run();
            return;
        }
        new Thread(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            onComplete.run();
        }).start();
    }
}
