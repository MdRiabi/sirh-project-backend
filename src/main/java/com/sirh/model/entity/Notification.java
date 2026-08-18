package com.sirh.model.entity;

import com.sirh.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "notifications")
@SQLDelete(sql = "UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "destinataire_id", nullable = false)
  private Long destinataireId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private NotificationType type;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "entite_type", length = 50)
  private String entiteType;

  @Column(name = "entite_id")
  private Long entiteId;

  @Column(nullable = false)
  @Builder.Default
  private Boolean lue = false;
}
