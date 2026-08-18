package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sirh.model.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("type")
  private NotificationType type;

  @JsonProperty("message")
  private String message;

  @JsonProperty("entite_type")
  private String entiteType;

  @JsonProperty("entite_id")
  private Long entiteId;

  @JsonProperty("lue")
  private Boolean lue;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
