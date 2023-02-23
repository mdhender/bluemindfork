/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.service;

import java.util.Date;
import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;

public interface IInCoreSmimeRevocation {

	/**
	 * Creates a new {@link SmimeRevocation} entry.
	 * 
	 * @param revocation value of the revocation
	 * @param cacertItem SmimeCacert attached to
	 * 
	 * @throws ServerFault
	 */
	void create(SmimeRevocation revocation, ItemValue<SmimeCacert> cacertItem) throws ServerFault;

	/**
	 * Fetch {@link SmimeRevocation} entry.
	 * 
	 * @param serialNumber certificate serial number
	 * @param cacertItem   S/MIME CA certificate
	 * @return {@link SmimeRevocation} entry
	 * 
	 * @throws ServerFault
	 */
	SmimeRevocation get(String serialNumber, ItemValue<SmimeCacert> cacertItem) throws ServerFault;

	/**
	 * Fetch revocated certificates by CA
	 * 
	 * @param cacertItem {@link SmimeCacert} entry
	 * 
	 * @throws ServerFault
	 */
	void fetchRevocations(ItemValue<SmimeCacert> cacertItem) throws ServerFault;

	/**
	 * Refresh revocated certificates by CA
	 * 
	 * @param cacertItem {@link SmimeCacert} entry
	 * 
	 * @throws ServerFault
	 */
	void refreshRevocations(ItemValue<SmimeCacert> cacertItem) throws ServerFault;

	/**
	 * Get S/MIME Certificate with next update date
	 * 
	 * @param update revoked certificate next update date
	 * 
	 * @throws ServerFault
	 */
	List<ItemValue<SmimeCacert>> getByNextUpdateDate(Date update) throws ServerFault;

}
