package org.example.moliyaapp.dto;

import lombok.*;
import jakarta.persistence.Entity;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EskizAuthResponse {
    private String message;
    private String token;

}
