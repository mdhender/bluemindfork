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
package net.bluemind.externaluser.service.internal;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.service.AbstractVCardAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.ExternalUser;

public class ExternalUserVCardAdapter extends AbstractVCardAdapter<ExternalUser> {

	@Override
	public VCard asVCard(ItemValue<Domain> domain, String uid, ExternalUser value) throws ServerFault {
		VCard card = value.contactInfos;
		card.source = "bm://" + domain.uid + "/externaluser/" + uid;
		return card;
	}

}
