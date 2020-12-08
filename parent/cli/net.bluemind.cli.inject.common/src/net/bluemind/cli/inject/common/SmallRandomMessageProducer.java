/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.inject.common;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class SmallRandomMessageProducer implements IMessageProducer {

	HashFunction h = Hashing.goodFastHash(64);

	@Override
	public byte[] createEml(TargetMailbox from, TargetMailbox to) {
		StringBuilder sb = new StringBuilder(1024);
		byte[] tgt = new byte[256];
		ThreadLocalRandom.current().nextBytes(tgt);

		sb.append("From: ").append(from.email).append("\r\n");
		sb.append("Subject: Hi ").append(to.email).append(' ').append(h.hashBytes(tgt).toString()).append("\r\n");
		sb.append("Content-Type: text/plain\r\n");
		sb.append("\r\n");
		sb.append(System.currentTimeMillis());
		sb.append("\r\n");
		return sb.toString().getBytes(StandardCharsets.US_ASCII);
	}

}
