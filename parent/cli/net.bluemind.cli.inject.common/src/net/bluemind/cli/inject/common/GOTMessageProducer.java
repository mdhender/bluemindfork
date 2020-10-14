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

import java.util.UUID;

import com.github.javafaker.Faker;
import com.github.javafaker.GameOfThrones;

public class GOTMessageProducer implements IMessageProducer {

	private static final GameOfThrones gotFaker = Faker.instance().gameOfThrones();

	@Override
	public byte[] createEml(TargetMailbox from, TargetMailbox to) {
		StringBuilder sb = new StringBuilder();
		sb.append("From: ").append(from.email).append("\r\n");
		sb.append("To: ").append(to.email).append("\r\n");
		sb.append("Content-Type: text/html; charset=utf-8\r\n");
		sb.append("Subject: Rand Message ").append(UUID.randomUUID()).append("\r\n\r\n");
		sb.append("<html><body><p>Yeah this is   body   </p>\r\n");
		for (int i = 0; i < 1024; i++) {
			sb.append("<p>").append(gotFaker.quote()).append("</p>\r\n");
			sb.append("<div>Written by <em>").append(gotFaker.character()).append("</em> of ").append(gotFaker.house())
					.append("</div>");
			sb.append("<div>Delivered to <em>").append(gotFaker.city()).append("</em></div>");
		}
		sb.append("\r\n</body></html>\r\n");
		return sb.toString().getBytes();
	}

}
