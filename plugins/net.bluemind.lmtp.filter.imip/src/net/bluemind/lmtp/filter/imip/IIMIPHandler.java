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
package net.bluemind.lmtp.filter.imip;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;

public interface IIMIPHandler {

	/**
	 * The handler is supposed to create/update the inBM event.
	 * 
	 * The book & cal objects can be used with the asUser token to executes the
	 * needed contact & calendar operations.
	 * 
	 * @param imip
	 * @param recipient
	 * @param domain
	 * @param iCalendarElement
	 * @param recipientDefaultCalendar
	 * @throws ServerFault
	 */
	IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox);

}
