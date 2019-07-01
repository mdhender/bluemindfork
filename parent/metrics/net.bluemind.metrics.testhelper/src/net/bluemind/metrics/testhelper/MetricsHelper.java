/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.testhelper;

import com.netflix.spectator.api.CompositeRegistry;
import com.netflix.spectator.api.Spectator;

public class MetricsHelper {

	private static TestRegistry testRegistry;

	public static TestRegistry beforeTests() {
		testRegistry = new TestRegistry();
		CompositeRegistry reg = Spectator.globalRegistry();
		reg.add(testRegistry);
		return testRegistry;
	}

	public static void afterTests() {
		CompositeRegistry reg = Spectator.globalRegistry();
		reg.remove(testRegistry);
	}
}
