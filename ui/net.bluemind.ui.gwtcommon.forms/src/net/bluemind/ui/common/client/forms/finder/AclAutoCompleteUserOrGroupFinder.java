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
package net.bluemind.ui.common.client.forms.finder;

import java.util.ArrayList;
import java.util.Arrays;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.ui.common.client.forms.Ajax;

public class AclAutoCompleteUserOrGroupFinder extends DirEntryFinder {

	public AclAutoCompleteUserOrGroupFinder() {
		super(Arrays.asList(DirEntry.Kind.USER, DirEntry.Kind.GROUP));
	}

	public AclAutoCompleteUserOrGroupFinder(int limit) {
		super(Arrays.asList(DirEntry.Kind.USER, DirEntry.Kind.GROUP), limit);
	}

	@Override
	public void find(DirEntryQuery tQuery, AsyncHandler<ListResult<DirEntry>> cb) {

		tQuery.hiddenFilter = !Ajax.TOKEN.isAdmin();
		directory.search(tQuery, new AsyncHandler<ListResult<ItemValue<DirEntry>>>() {

			@Override
			public void success(ListResult<ItemValue<DirEntry>> value) {
				ListResult<DirEntry> ret = new ListResult<>();
				ret.total = value.total;
				ret.values = new ArrayList<>(value.values.size());
				for (ItemValue<DirEntry> entry : value.values) {
					ret.values.add(entry.value);
				}

				cb.success(ret);
			}

			@Override
			public void failure(Throwable e) {
				cb.failure(e);
			}
		});
	}

}
