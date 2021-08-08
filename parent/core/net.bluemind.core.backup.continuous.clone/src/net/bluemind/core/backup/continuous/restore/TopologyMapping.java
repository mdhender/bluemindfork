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
package net.bluemind.core.backup.continuous.restore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;

public class TopologyMapping {

	private Properties props;

	public TopologyMapping(Path propsFile) throws IOException {
		this(load(propsFile));
	}

	public TopologyMapping(Properties props) {
		this.props = props;
	}

	public TopologyMapping(Map<String, String> props) {
		this();
		props.forEach(this::register);
	}

	public TopologyMapping() {
		this(new Properties());
	}

	@VisibleForTesting
	public void register(String uid, String ip) {
		props.put(uid, ip);
	}

	private static Properties load(Path propsFile) throws IOException {
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(propsFile)) {
			props.load(in);
		}
		return props;
	}

	public String ipAddressForUid(String uid, String backupIp) {
		return props.getProperty(uid, backupIp);
	}

}
