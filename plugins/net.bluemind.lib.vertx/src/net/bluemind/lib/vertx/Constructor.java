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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx;

import java.util.function.Supplier;

import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.VerticleConstructor;

public class Constructor {

	public static VerticleConstructor of(Supplier<Verticle> instance, Class<?> vClass) {

		return new VerticleConstructor() {

			private final String cn = vClass.getCanonicalName();

			@Override
			public Verticle newInstance() throws Exception {
				return instance.get();
			}

			@Override
			public String className() {
				return cn;
			}
		};
	}

}
