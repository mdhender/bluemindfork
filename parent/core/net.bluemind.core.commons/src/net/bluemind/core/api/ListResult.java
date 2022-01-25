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
package net.bluemind.core.api;

import java.util.Collections;
import java.util.List;

@BMApi(version = "3")
public class ListResult<E> {

	public List<E> values;
	public long total;

	public ListResult() {
		values = Collections.emptyList();
		total = 0;
	}

	public static <E> ListResult<E> create(List<E> values) {
		return create(values, values.size());
	}

	public static <E> ListResult<E> create(List<E> values, long total) {
		ListResult<E> ret = new ListResult<>();
		ret.total = total;
		ret.values = values;
		return ret;
	}

}
