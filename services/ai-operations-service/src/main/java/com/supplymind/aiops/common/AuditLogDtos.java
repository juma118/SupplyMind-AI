package com.supplymind.aiops.common;

import java.time.Instant;
import java.util.UUID;

public final class AuditLogDtos {

    private AuditLogDtos() {
    }

    public record AuditLogResponse(
            UUID id,
            String actor,
            String action,
            String entityType,
            String entityId,
            String details,
            Instant occurredAt) {

        public static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(log.getId(), log.getActor(), log.getAction(), log.getEntityType(),
                    log.getEntityId(), log.getDetails(), log.getOccurredAt());
        }
    }
}
