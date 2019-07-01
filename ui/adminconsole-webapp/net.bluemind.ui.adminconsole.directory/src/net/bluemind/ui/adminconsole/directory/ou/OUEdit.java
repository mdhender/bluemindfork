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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.domain.api.Domain;
import net.bluemind.ui.common.client.forms.autocomplete.EntityEdit;

public class OUEdit extends EntityEdit<OrgUnitPath, OrgUnitQuery> {

	public OUEdit() {
		super(new OUFinder(), false, false, null);
	}

	public void setDomain(ItemValue<Domain> domain) {
		((OUFinder) getFinder()).setDomain(domain.uid);

	}

	public void setDomain(String domainUid) {
		((OUFinder) getFinder()).setDomain(domainUid);
	}

	public void setKind(DirEntry.Kind kind) {
		((OUFinder) getFinder()).setKind(kind);
	}

}
