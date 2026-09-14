package com.agente.agente_ia_local;

import com.agente.agente_ia_local.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MessageDebounceServiceTest {

    @Autowired private MessageDebounceService debounceService;

    @Test
    @DisplayName("Un solo mensaje se procesa después del debounce")
    void singleMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<String> receivedMessages = new CopyOnWriteArrayList<>();

        debounceService.addMessage("tenant1", "+549111111", "Hola", (phone, messages) -> {
            receivedMessages.addAll(messages);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Callback debe ser invocado");
        assertEquals(1, receivedMessages.size());
        assertEquals("Hola", receivedMessages.get(0));
    }

    @Test
    @DisplayName("Múltiples mensajes rápidos se fusionan")
    void multipleMessagesFused() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<String> receivedMessages = new CopyOnWriteArrayList<>();

        debounceService.addMessage("tenant2", "+549111111", "Hola", (phone, messages) -> {
            receivedMessages.addAll(messages);
            latch.countDown();
        });

        Thread.sleep(30);
        debounceService.addMessage("tenant2", "+549111111", "¿Tienen zapatos?", (phone, messages) -> {
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback debe ser invocado después del debounce");
        assertTrue(receivedMessages.size() >= 2, "Deben fusionarse al menos 2 mensajes, got: " + receivedMessages.size());
    }

    @Test
    @DisplayName("Mensajes de diferentes usuarios se procesan independientemente")
    void differentUsersSeparate() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CopyOnWriteArrayList<String> msgs1 = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> msgs2 = new CopyOnWriteArrayList<>();

        debounceService.addMessage("tenant3", "+549111111", "Msg 1", (phone, messages) -> {
            msgs1.addAll(messages);
            latch.countDown();
        });

        debounceService.addMessage("tenant3", "+549111222", "Msg 2", (phone, messages) -> {
            msgs2.addAll(messages);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertFalse(msgs1.isEmpty());
        assertFalse(msgs2.isEmpty());
    }

    @Test
    @DisplayName("Mensajes de diferentes tenants se procesan independientemente")
    void differentTenantsSeparate() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CopyOnWriteArrayList<String> msgs1 = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> msgs2 = new CopyOnWriteArrayList<>();

        debounceService.addMessage("tenantA", "+549111111", "Msg A", (phone, messages) -> {
            msgs1.addAll(messages);
            latch.countDown();
        });

        debounceService.addMessage("tenantB", "+549111111", "Msg B", (phone, messages) -> {
            msgs2.addAll(messages);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertFalse(msgs1.isEmpty());
        assertFalse(msgs2.isEmpty());
    }
}
