/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.exec;

import net.bluemind.imap.endpoint.cmd.ExpungeCommand;
import net.bluemind.imap.endpoint.driver.ImapIdSet;

public class ExpungeProcessor extends AbstractExpungeProcessor<ExpungeCommand> {

	@Override
	public Class<ExpungeCommand> handledType() {
		return ExpungeCommand.class;
	}

	public ExpungeProcessor() {
		super(true);
	}

	@Override
	protected ImapIdSet fromSet(ExpungeCommand command) {
		return ImapIdSet.uids("1:*");
	}

}
