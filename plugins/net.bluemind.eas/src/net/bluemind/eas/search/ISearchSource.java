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
package net.bluemind.eas.search;

import java.util.Collection;
import java.util.LinkedList;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.dto.search.StoreName;

public interface ISearchSource {

	@SuppressWarnings("serial")
	public static class Results<E> extends LinkedList<E> {

		private long numFound = 0;

		public Results() {
			super();
		}

		public Results(Collection<? extends E> c) {
			super(c);
		}

		/**
		 * @param l
		 *            the real count ignoring pagination
		 * 
		 */
		public void setNumFound(long l) {
			this.numFound = l;
		}

		/**
		 * @return the real count ignoring pagination
		 */
		public long getNumFound() {
			if (numFound == 0) {
				return this.size();
			}
			return numFound;
		}

	}

	StoreName getStoreName();

	public Results<SearchResult> search(BackendSession bs, SearchRequest request);

}
