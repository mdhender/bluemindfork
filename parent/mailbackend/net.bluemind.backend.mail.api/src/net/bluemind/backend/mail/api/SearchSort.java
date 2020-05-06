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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SearchSort {
	public List<SortCriteria> criteria = Collections.emptyList();

	@BMApi(version = "3")
	public static class SortCriteria {
		public String field;
		public Order order;
	}

	@BMApi(version = "3")
	public enum Order {
		Asc, Desc;
	}

	public static SearchSort byField(String field, Order o) {
		SearchSort s = new SearchSort();
		SortCriteria ss = new SortCriteria();
		ss.field = field;
		ss.order = o;
		s.criteria = Arrays.asList(ss);
		return s;
	}

	public boolean hasCriterias() {
		return !criteria.isEmpty();
	}

}
