package com.agente.agente_ia_local.dto;

import java.time.LocalDateTime;

public class ChatResponseDTO {

    private Long id;
    private String prompt;
    private String response;
    private LocalDateTime createdAt;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(Long id, String prompt, String response, LocalDateTime createdAt) {
        this.id = id;
        this.prompt = prompt;
        this.response = response;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
