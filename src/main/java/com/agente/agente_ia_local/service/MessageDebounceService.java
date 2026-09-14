package com.agente.agente_ia_local.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class MessageDebounceService {

    private static final Logger log = LoggerFactory.getLogger(MessageDebounceService.class);

    @Value("${chat.debounce.window-ms:3500}")
    private long debounceWindowMs;

    private final ConcurrentHashMap<String, DebounceEntry> pendingMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public interface DebouncedCallback {
        void process(String userPhone, List<String> messages);
    }

    public void addMessage(String tenantKey, String userPhone, String message, DebouncedCallback callback) {
        String key = tenantKey + ":" + userPhone;
        pendingMessages.compute(key, (k, existing) -> {
            DebounceEntry entry = (existing != null) ? existing : new DebounceEntry(callback);
            entry.messages.add(message);
            if (entry.future != null) {
                entry.future.cancel(false);
            }
            entry.future = scheduler.schedule(() -> {
                DebounceEntry e = pendingMessages.remove(key);
                if (e != null) {
                    log.debug("Debounce complète para {}, {} mensajes fusionados", userPhone, e.messages.size());
                    e.callback.process(userPhone, new ArrayList<>(e.messages));
                }
            }, debounceWindowMs, TimeUnit.MILLISECONDS);
            return entry;
        });
    }

    private static class DebounceEntry {
        List<String> messages = new CopyOnWriteArrayList<>();
        DebouncedCallback callback;
        ScheduledFuture<?> future;

        DebounceEntry(DebouncedCallback callback) {
            this.callback = callback;
        }
    }
}
