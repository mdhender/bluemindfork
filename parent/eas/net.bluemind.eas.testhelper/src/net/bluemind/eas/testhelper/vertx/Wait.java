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
package net.bluemind.eas.testhelper.vertx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;

public class Wait {

	public static void forIt(CountDownLatch cdl) {
		try {
			boolean finished = cdl.await(20, TimeUnit.SECONDS);
			if (!finished) {
				throw new RuntimeException("Steps not completed in time");
			}
		} catch (InterruptedException e) {
			Throwables.propagate(e);
		}
	}

}
