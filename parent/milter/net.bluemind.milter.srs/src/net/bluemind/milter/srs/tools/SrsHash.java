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
package net.bluemind.milter.srs.tools;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class SrsHash {
	private static final Logger logger = LoggerFactory.getLogger(SrsHash.class);

	private static final int length = 4; // Keep only this length of base64 encoded MAC
	private static final String algorithm = "HmacSHA1";

	private final Mac mac;

	public static Optional<SrsHash> build(String key) {
		if (Strings.isNullOrEmpty(key)) {
			return Optional.empty();
		}

		try {
			Mac mac = Mac.getInstance(algorithm);
			mac.init(new SecretKeySpec(key.getBytes(), algorithm));
			return Optional.of(new SrsHash(mac));
		} catch (NoSuchAlgorithmException e) {
			logger.error("Invalid SRS Mac algorithm {}", algorithm, e);
			return Optional.empty();
		} catch (InvalidKeyException e) {
			logger.error("Invalid SRS Mac key {}", key, e);
			return Optional.empty();
		}
	}

	private SrsHash(Mac mac) {
		this.mac = mac;
	}

	public String encode(String timestamp, String localPart, String hostname) {
		byte[] hash = mac.doFinal(new StringBuilder().append(timestamp).append(hostname).append(localPart).toString()
				.toLowerCase().getBytes());

		return new String(Base64.getEncoder().encode(hash)).substring(0, length);
	}

	public boolean check(String expected, String timestamp, String localPart, String hostname) {
		if (Strings.isNullOrEmpty(expected) || expected.length() != 4) {
			return false;
		}

		// Check case insensitively as some MTA may smashed case
		// http://www.open-spf.org/srs/srs.pdf (4.1)
		// opensrsd:
		// get SRS0=Vkhu=RG=domain.tld=toto@apr-vmnet.loc
		// 200 toto@domain.tld
		// get SRS0=vkhu=RG=domain.tld=toto@apr-vmnet.loc
		// 200 toto@domain.tld
		// get SRS0=VKHU=RG=domain.tld=toto@apr-vmnet.loc
		// 200 toto@domain.tld
		// get SRS0=vfHU=RG=domain.tld=toto@apr-vmnet.loc
		// 500 Hash invalid in SRS address.
		return expected.equalsIgnoreCase(encode(timestamp, localPart, hostname));
	}
}
