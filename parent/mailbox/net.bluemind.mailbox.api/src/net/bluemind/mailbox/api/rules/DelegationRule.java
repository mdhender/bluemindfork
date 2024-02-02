/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.mailbox.api.rules;

import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * Represent a delegation rule between a delegator and its delegates<br/>
 * A delegator is the boss who give access to its mailbox to to others
 * (delegates)
 */
@BMApi(version = "3")
public class DelegationRule {

	public String delegatorCalendarUid;
	public List<String> delegateUids;
	public String delegatorUid;
	public boolean keepCopy;
	public boolean readOnly;

	public DelegationRule() {

	}

	public DelegationRule(String delegatorCalendarUid, List<String> delegateUids, String delegatorUid, boolean keepCopy,
			boolean readOnly) {
		this.delegatorCalendarUid = delegatorCalendarUid;
		this.delegateUids = delegateUids;
		this.delegatorUid = delegatorUid;
		this.keepCopy = keepCopy;
		this.readOnly = readOnly;
	}

}
