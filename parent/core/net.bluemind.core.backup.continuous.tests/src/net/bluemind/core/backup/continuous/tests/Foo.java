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
package net.bluemind.core.backup.continuous.tests;

import java.util.concurrent.ThreadLocalRandom;

import com.google.common.hash.Hashing;

public class Foo {

	public String bar;
	public int baz;
	public byte[] blob;

	public static Foo random() {
		Foo kushima = new Foo();
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		kushima.bar = Hashing.goodFastHash(64).hashLong(rand.nextLong()).toString();
		kushima.baz = rand.nextInt();
		kushima.blob = new byte[1024];
		rand.nextBytes(kushima.blob);
		return kushima;
	}

}
