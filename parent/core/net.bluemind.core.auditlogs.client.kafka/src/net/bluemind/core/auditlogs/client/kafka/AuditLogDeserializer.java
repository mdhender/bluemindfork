package net.bluemind.core.auditlogs.client.kafka;

import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

class AuditLogDeserializer {

	private final ValueReader<AuditLogKey> keyReader;

	public AuditLogDeserializer() {
		this.keyReader = JsonUtils.reader(AuditLogKey.class);
	}

	public AuditLogKey key(byte[] k) {
		return keyReader.read(new String(k));
	}

	public AuditLogEntry value(byte[] data) {
		try {
			return JsonUtils.read(new String(data), AuditLogEntry.class);
		} catch (Exception e) {
			return null;
		}
	}

}