package net.bluemind.pool.impl.docker;

public enum DockerContainer {

	ELASTICSEARCH("bluemind/elasticsearch-tests", "es-host"),

	POSTGRES("bluemind/postgres-tests", "host"),

	POSTGRES_MIGRATION("bluemind/postgres-migration", "host"),

	NODE("bluemind/node-tests", "node-host"),

	SMTP_ROLE("bluemind/smtp-role", "smtp-role"),

	SMTP_EDGE("bluemind/smtp-edge-role", "smtp-edge-role"),

	IMAP("bluemind/imap-role", "imap-role"),

	LDAP("bluemind/ldap", "ldap"),

	SAMBA4("bluemind/samba4-import", "samba4-import"),

	WEBDAV("bluemind/webdav", "webdav"),

	S3("bluemind/s3", "s3"),

	PROXY("bluemind/proxy", "proxy"),

	SCALITYRING("bluemind/scalityring", "scalityring");

	private final String name;
	private final String hostProperty;

	private DockerContainer(String name, String hostProperty) {
		this.name = name;
		this.hostProperty = hostProperty;
	}

	public String getName() {
		return this.name;
	}

	public String getHostProperty() {
		return hostProperty;
	}

}
