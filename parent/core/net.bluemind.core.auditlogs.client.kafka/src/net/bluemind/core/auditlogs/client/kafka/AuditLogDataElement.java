package net.bluemind.core.auditlogs.client.kafka;

public record AuditLogDataElement(AuditLogKey key, byte[] payload, int part, long offset) {
}
