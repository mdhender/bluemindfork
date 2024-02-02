package net.bluemind.core.auditlogs.client.kafka;

public record AuditLogKey(String containerUid, String itemUid, long time) {
}
