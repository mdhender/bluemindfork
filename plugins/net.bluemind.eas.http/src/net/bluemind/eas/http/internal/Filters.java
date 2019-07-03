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
package net.bluemind.eas.http.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.bluemind.eas.http.IEasRequestFilter;
import net.bluemind.eas.utils.RunnableExtensionLoader;

public class Filters {

	private static List<IEasRequestFilter> filters;

	public static List<IEasRequestFilter> get() {
		return filters;
	}

	public static void classLoad() {
		RunnableExtensionLoader<IEasRequestFilter> rel = new RunnableExtensionLoader<>();
		filters = rel.loadExtensions("net.bluemind.eas.http", "endpoint", "filter", "impl");
		Collections.sort(filters, new Comparator<IEasRequestFilter>() {

			@Override
			public int compare(IEasRequestFilter o1, IEasRequestFilter o2) {
				return new Integer(o1.priority()).compareTo(new Integer(o2.priority()));
			}
		});
	}

}
