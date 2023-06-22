/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.application.registration.model;

public class ApplicationMetric {

	public String key;
	public long value;
	public AppTag tag;

	public enum AppTag {
		TAIL, MASTER;
	}

	public static ApplicationMetric create(String key, long value, AppTag tag) {
		ApplicationMetric appMetric = new ApplicationMetric();
		appMetric.key = key;
		appMetric.value = value;
		appMetric.tag = tag;
		return appMetric;
	}
}
