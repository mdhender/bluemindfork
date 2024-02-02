package net.bluemind.core.auditlogs.client.kafka.config.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.core.auditlogs.client.kafka.config.AuditLogKafkaConfig;

public class AuditLogKafkaConfigTests {

	@Test
	public void loadDefaultConfig() {

		assertEquals("86400000", AuditLogKafkaConfig.getSegmentTimeMs());
		assertEquals("259200000", AuditLogKafkaConfig.getRetentionTimeMs());
		assertEquals("134217728", AuditLogKafkaConfig.getSegmentSizeByte());
	}

}
