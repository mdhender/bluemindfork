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

import net.bluemind.imap.endpoint.cmd.UidExpungeCommand;
import net.bluemind.imap.endpoint.driver.ImapIdSet;
import net.bluemind.imap.endpoint.locks.ISequenceCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceWriter;

public class UidExpungeProcessor extends AbstractExpungeProcessor<UidExpungeCommand>
		implements ISequenceWriter, ISequenceCheckpoint {

	public UidExpungeProcessor() {
		super(false);
	}

	@Override
	protected ImapIdSet fromSet(UidExpungeCommand command) {
		return ImapIdSet.uids(command.idset());
	}
	
	@Override
	public Class<UidExpungeCommand> handledType() {
		return UidExpungeCommand.class;
	}

}
