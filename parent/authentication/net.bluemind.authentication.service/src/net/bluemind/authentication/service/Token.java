/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.service;

import java.util.concurrent.TimeUnit;

import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;

@HollowPrimaryKey(fields = { "key" })
public class Token {

	public String key;

	public String subjectUid;

	public String subjectDomain;

	public long expiresTimestamp;

	public Token(String key, String subject, String subjectDomain) {
		this.key = key;
		this.subjectUid = subject;
		this.subjectDomain = subjectDomain;
		expiresTimestamp = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7);
	}

	public void renew() {
		expiresTimestamp = expiresTimestamp + TimeUnit.DAYS.toMillis(7);
	}
}