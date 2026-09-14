package com.agente.agente_ia_local;

import com.agente.agente_ia_local.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "chat.typing.enabled=true")
class TypingIndicatorServiceTest {

    @Autowired private TypingIndicatorService typingService;

    @Test
    @DisplayName("Delay para mensaje corto (5 palabras)")
    void shortMessage() {
        long delay = typingService.calculateTypingDelay("Hola, ¿cómo estás?");
        assertTrue(delay > 0, "Short message should have positive delay");
        assertTrue(delay < 5000, "Delay should be under 5s for short messages");
    }

    @Test
    @DisplayName("Delay para mensaje largo (50 palabras)")
    void longMessage() {
        String longMsg = "palabra ".repeat(50);
        long delay = typingService.calculateTypingDelay(longMsg);
        assertTrue(delay > 0);
        assertTrue(delay <= 6000, "Delay should be max 6s");
    }

    @Test
    @DisplayName("Delay máximo respetado")
    void maxDelayRespected() {
        String hugeMsg = "word ".repeat(200);
        long delay = typingService.calculateTypingDelay(hugeMsg);
        assertTrue(delay <= 6000, "Delay should never exceed max");
    }

    @Test
    @DisplayName("Texto null retorna 0")
    void nullText() {
        long delay = typingService.calculateTypingDelay(null);
        assertEquals(0, delay);
    }

    @Test
    @DisplayName("Texto vacío retorna 0")
    void emptyText() {
        long delay = typingService.calculateTypingDelay("");
        assertEquals(0, delay);
    }
}
