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
package net.bluemind.resource.service.internal;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.service.AbstractVCardAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.resource.api.ResourceDescriptor;

public class ResourceVCardAdapter extends AbstractVCardAdapter<ResourceDescriptor> {

	@Override
	public VCard asVCard(ItemValue<Domain> domain, String uid, ResourceDescriptor rd) throws ServerFault {
		VCard card = new VCard();
		card.identification.formatedName = VCard.Identification.FormatedName.create(rd.label);
		// prevent to be sanitized...
		card.identification.name = VCard.Identification.Name.create(rd.label, null, null, null, null, null);
		card.source = "bm://" + domain.uid + "/resources/" + uid;
		card.communications.emails = getEmails(domain, rd.emails);
		card.explanatory.note = rd.description;
		return card;
	}

}
