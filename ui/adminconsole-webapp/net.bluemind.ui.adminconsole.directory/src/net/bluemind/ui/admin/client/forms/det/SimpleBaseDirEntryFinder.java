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
package net.bluemind.ui.admin.client.forms.det;

import java.util.List;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;

public interface SimpleBaseDirEntryFinder {

	public void find(DirEntryQuery tQuery, final AsyncHandler<ListResult<DirEntry>> cb);

	public void setDomain(String domain);

	public void setFilterOut(List<String> filterOut);

}
