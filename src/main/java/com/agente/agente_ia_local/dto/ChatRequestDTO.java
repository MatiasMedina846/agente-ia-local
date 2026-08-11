package com.agente.agente_ia_local.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequestDTO {

    @NotBlank(message = "El prompt no puede ser nulo ni vacío")
    private String prompt;

    public ChatRequestDTO() {
    }

    public ChatRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
