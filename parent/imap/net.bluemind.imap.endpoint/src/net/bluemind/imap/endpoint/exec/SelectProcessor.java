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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.exec;

import net.bluemind.imap.endpoint.cmd.SelectCommand;

/**
 * 
 * <code>
 * * 7 EXISTS
 * 0 RECENT
 * FLAGS (\Answered \Flagged \Draft \Deleted \Seen)
 * OK [PERMANENTFLAGS (\Answered \Flagged \Draft \Deleted \Seen \*)] Ok
 * OK [UNSEEN 1] Ok
 * OK [UIDVALIDITY 1626984990] Ok
 * OK [UIDNEXT 28] Ok
 * OK [HIGHESTMODSEQ 3231] Ok
 * OK [URLMECH INTERNAL] Ok
 * OK [ANNOTATIONS 65536] Ok
 * . OK [READ-WRITE] Completed
 * </code>
 * 
 * 
 * <code>
 * . select "Dossiers partag&AOk-s/read.only"
 * * 1 EXISTS
 * * 0 RECENT
 * * FLAGS (\Answered \Flagged \Draft \Deleted \Seen)
 * * OK [PERMANENTFLAGS ()] Ok
 * * OK [UNSEEN 1] Ok
 * * OK [UIDVALIDITY 1626984990] Ok
 * * OK [UIDNEXT 2] Ok
 * * OK [HIGHESTMODSEQ 7873668] Ok
 * * OK [URLMECH INTERNAL] Ok
 * * OK [ANNOTATIONS 65536] Ok
 * . OK [READ-ONLY] Completed
 * </code>
 * 
 *
 */
public class SelectProcessor extends AbstractSelectorProcessor<SelectCommand> {
	@Override
	public Class<SelectCommand> handledType() {
		return SelectCommand.class;
	}

	@Override
	protected boolean isAlwaysReadOnly() {
		return false;
	}
}
