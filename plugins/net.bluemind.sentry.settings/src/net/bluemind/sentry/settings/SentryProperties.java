/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sentry.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.sentry.Sentry;

public class SentryProperties {
	private static final Path SENTRY_CONF_DIR = Paths.get("/etc/bm/sentry");
	private static final Path SENTRY_TEMP_PARENT_DIR = Paths.get("/tmp/sentry");
	private static final String SENTRY_CONF_FMT = "%s/%s.properties";
	private static final Path SENTRY_TEMP_PATH = Paths.get(String.format("%s/%s", SENTRY_TEMP_PARENT_DIR,
			System.getProperty("net.bluemind.property.product", "unknown")));

	public static final String PERMISSIONS_SENTRY_SETTINGS_FOLDER = "rwxrwxrwx";
	public static final String PERMISSIONS_SENTRY_TEMP_PARENT_FOLDER = "rwxrwxrwx";
	public static final String PERMISSIONS_PROPERTIES = "rw-r-----";
	public static final String PERMISSIONS_TEMPEVENTS = "rwxrwx---";

	private final Properties props = new Properties();

	public SentryProperties() {
		props.setProperty("dsn", "");
		props.setProperty("samplerate", "1");
		props.setProperty("uncaught.handler.enabled", "true");
		props.setProperty("buffer.dir", SENTRY_TEMP_PATH.toAbsolutePath().toString());
		props.setProperty("maxmessagelength", "2000");
		props.setProperty("environment", "BM_COMMUNITY");
		props.setProperty("release", "UNKNOWN_RELEASE");
		props.setProperty("servername", "UNKNOWN_SERVER");
		props.setProperty("stacktrace.app.packages", "net.bluemind");
		try {
			load();
		} catch (IOException ioe) {
			// ok
		}
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			props.setProperty("tags",
					String.format("serverip:%s,serverhostname:%s,product:%s", localhost.getHostAddress(),
							localhost.getHostName(), System.getProperty("net.bluemind.property.product", "unknown")));
		} catch (UnknownHostException e) {
			// doesn't matter that much
		}
	}

	public void setDsn(String dsn) {
		props.setProperty("dsn", dsn);
	}

	public void setServerName(String serverName) {
		props.setProperty("servername", serverName);
	}

	public void setEnvironment(String environment) {
		props.setProperty("environment", environment);
	}

	public void setRelease(String release) {
		props.setProperty("release", release);
	}

	public static void checkOrCreateFolders() throws IOException {
		Set<PosixFilePermission> sentryTempPermissions = PosixFilePermissions.fromString(PERMISSIONS_TEMPEVENTS);
		Set<PosixFilePermission> sentryConfdirPermissions = PosixFilePermissions
				.fromString(PERMISSIONS_SENTRY_SETTINGS_FOLDER);
		Set<PosixFilePermission> sentryTempParentPermissions = PosixFilePermissions
				.fromString(PERMISSIONS_SENTRY_TEMP_PARENT_FOLDER);

		if (!Files.exists(SENTRY_TEMP_PARENT_DIR)) {
			Files.createDirectories(SENTRY_TEMP_PARENT_DIR,
					PosixFilePermissions.asFileAttribute(sentryTempParentPermissions));
		} else {
			try {
				Files.setPosixFilePermissions(SENTRY_TEMP_PARENT_DIR, sentryTempParentPermissions);
			} catch (Exception e) {
				// permission denied for sds-proxy, just continue
			}
		}

		if (!Files.exists(SENTRY_TEMP_PATH)) {
			Files.createDirectories(SENTRY_TEMP_PATH, PosixFilePermissions.asFileAttribute(sentryTempPermissions));
		} else {
			try {
				Files.setPosixFilePermissions(SENTRY_TEMP_PATH, sentryTempPermissions);
			} catch (Exception e) {
				// permission denied for sds-proxy, just continue
			}
		}

		if (!Files.exists(SENTRY_CONF_DIR)) {
			Files.createDirectories(SENTRY_CONF_DIR, PosixFilePermissions.asFileAttribute(sentryConfdirPermissions));
		} else {
			try {
				Files.setPosixFilePermissions(SENTRY_CONF_DIR, sentryConfdirPermissions);
			} catch (Exception e) {
				// permission denied for sds-proxy, just continue
			}
		}
	}

	public static Path getConfigurationPath() {
		return Paths.get(String.format(SENTRY_CONF_FMT, SENTRY_CONF_DIR.toAbsolutePath().toString(),
				System.getProperty("net.bluemind.property.product", "unknown-jvm")));
	}

	public boolean enabled() {
		return !props.getProperty("dsn").isEmpty();
	}

	public void update() throws IOException {
		updateSystem();
		updatePropertiesFile();
	}

	private void updateSystem() {
		for (Map.Entry<Object, Object> prop : props.entrySet()) {
			System.setProperty("sentry." + prop.getKey(), prop.getValue().toString());
		}
	}

	private void load() throws IOException {
		Path sentryConfigurationPath = getConfigurationPath();
		if (Files.exists(sentryConfigurationPath)) {
			try (InputStream in = Files.newInputStream(sentryConfigurationPath)) {
				props.load(in);
				updateSystem();
			}
		}
	}

	private void updatePropertiesFile() throws IOException {
		Path sentryConfigurationPath = getConfigurationPath();
		Set<PosixFilePermission> propertiesPermissions = PosixFilePermissions.fromString(PERMISSIONS_PROPERTIES);
		try (OutputStream out = Files.newOutputStream(sentryConfigurationPath, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			props.store(out, null);
		}
		Files.setPosixFilePermissions(sentryConfigurationPath, propertiesPermissions);
	}

	public void close() {
		Sentry.close();
		System.setProperty("sentry.dsn", "");
	}

}
