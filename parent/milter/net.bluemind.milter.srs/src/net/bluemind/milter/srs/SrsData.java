/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.milter.srs;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.Regex;
import net.bluemind.milter.srs.tools.SrsHash;
import net.bluemind.milter.srs.tools.SrsTimestamp;

public class SrsData {
	private static final Logger logger = LoggerFactory.getLogger(SrsData.class);

	private static final String PREFIX = "SRS0";
	private static final String SEP = "=";

	public final String hash;
	public final String timestamp;
	public final String hostname;
	public final String localPart;

	private SrsData(String hash, String timestamp, String localPart, String hostname) {
		this.hash = hash;
		this.timestamp = timestamp;
		this.hostname = hostname;
		this.localPart = localPart;
	}

	public static Optional<SrsData> forEmail(SrsHash srsHash, String email) {
		if (Strings.isNullOrEmpty(email) || !Regex.EMAIL.validate(email)) {
			return Optional.empty();
		}

		String timeStamp = SrsTimestamp.from(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1));

		String[] parts = email.split("@");
		String hash = srsHash.encode(timeStamp, parts[0], parts[1]);

		return Optional.of(new SrsData(hash, timeStamp, parts[0], parts[1]));
	}

	public static Optional<SrsData> fromLeftPart(SrsHash srsHash, String leftPart) {
		if (Strings.isNullOrEmpty(leftPart) || !leftPart.startsWith(PREFIX)) {
			logger.debug("SRS part {} is invalid", leftPart);
			return Optional.empty();
		}

		String[] srsParts = leftPart.substring(PREFIX.length() + SEP.length()).split(SEP, 4);
		if (srsParts.length != 4) {
			logger.debug("SRS part {} is invalid", leftPart);
			return Optional.empty();
		}

		if (!SrsTimestamp.check(srsParts[1])) {
			logger.debug("SRS address too old {}", leftPart);
			return Optional.empty();
		}

		if (!srsHash.check(srsParts[0], srsParts[1], srsParts[3], srsParts[2])) {
			logger.debug("SRS hash is invalid {}", leftPart);
			return Optional.empty();
		}

		return Optional.of(new SrsData(srsParts[0], srsParts[1], srsParts[3], srsParts[2]));
	}

	public String srsEmail(String domain) {
		return new StringBuilder().append(PREFIX).append(SEP).append(hash).append(SEP).append(timestamp).append(SEP)
				.append(hostname).append(SEP).append(localPart).append("@").append(domain).toString();
	}

	public String originalEmail() {
		return new StringBuilder().append(localPart).append("@").append(hostname).toString();
	}
}
