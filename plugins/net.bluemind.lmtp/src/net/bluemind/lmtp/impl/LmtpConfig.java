/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lmtp.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lmtp.Activator;
import net.bluemind.lmtp.backend.IMessageFilter;

public class LmtpConfig {
	private static final Logger logger = LoggerFactory.getLogger(LmtpConfig.class);

	private String recipientDelimiter;
	private String name;
	private static final String defaultVersion = "0.0.1";
	private static String fullVersion;

	private List<IMessageFilter> filters;

	static {
		fullVersion = defaultVersion;
		InputStream in = LmtpConfig.class.getClassLoader().getResourceAsStream("data/version.properties");
		Properties p = new Properties();
		try {
			p.load(in);
			String fromFile = p.getProperty("version");
			if (fromFile == null) {
				logger.warn("version property not found. Defaulting to " + defaultVersion);
			} else {
				fullVersion = fromFile;
			}
		} catch (IOException e) {
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	public LmtpConfig() {
		this.name = getServerName();
		this.recipientDelimiter = "+";
		this.filters = Activator.getDefault().getLmtpFilters();
	}

	public static String getServerName() {
		return "BM-lmtpd";
	}

	public static String getServerVersion() {
		return fullVersion;
	}

	public String getRecipientDelimiter() {
		return recipientDelimiter;
	}

	public boolean permanentFailureWhenOverQuota() {
		return false;
	}

	public String getName() {
		return name;
	}

	public List<IMessageFilter> getFilters() {
		return filters;
	}
}
