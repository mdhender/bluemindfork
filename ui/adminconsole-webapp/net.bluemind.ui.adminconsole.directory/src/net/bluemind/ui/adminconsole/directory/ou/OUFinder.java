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
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder;

public class OUFinder implements IEntityFinder<OrgUnitPath, OrgUnitQuery> {

	private String domainUid;
	private Kind kind;

	@Override
	public String getType(OrgUnitPath result) {
		return "ORG_UNIT";
	}

	@Override
	public String toString(OrgUnitPath result) {
		String name = null;
		for (OrgUnitPath p = result; p != null; p = p.parent) {
			if (name == null) {
				name = p.name;
			} else {
				name = p.name + "/" + name;
			}
		}
		return name;
	}

	@Override
	public void find(OrgUnitQuery tQuery, AsyncHandler<ListResult<OrgUnitPath>> cb) {
		if (domainUid == null) {
			domainUid = "global.virt";
		}

		if (kind != null) {
			tQuery.managableKinds = new HashSet<>(Arrays.asList(kind));
		}
		IOrgUnitsPromise ous = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		ous.search(tQuery).thenApply(res -> {
			cb.success(ListResult.create(res));
			return null;
		}).exceptionally(t -> {
			cb.failure(t);
			return null;
		});
	}

	@Override
	public OrgUnitQuery queryFromString(String queryString) {
		OrgUnitQuery q = new OrgUnitQuery();
		q.size = 10;
		q.query = queryString;
		return q;
	}

	@Override
	public void reload(Collection<OrgUnitPath> ids,
			net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder.ReloadCb<OrgUnitPath> cb) {
		// nothing to do
	}

	@Override
	public void setDomain(String domain) {
		this.domainUid = domain;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}
}
