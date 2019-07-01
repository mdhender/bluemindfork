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
package net.bluemind.ui.common.client.forms.autocomplete;

import java.util.Collection;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;

public interface IEntityFinder<T, TQ> {

	interface ReloadCb<W> {
		void finished(Collection<W> results);
	}

	String getType(T result);

	String toString(T result);

	void find(TQ tQuery, AsyncHandler<ListResult<T>> cb);

	TQ queryFromString(String queryString);

	void reload(Collection<T> ids, ReloadCb<T> cb);

	void setDomain(String domain);

}
