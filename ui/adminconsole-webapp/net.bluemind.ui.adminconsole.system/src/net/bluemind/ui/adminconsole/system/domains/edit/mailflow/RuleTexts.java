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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow;

import com.google.gwt.core.client.GWT;

import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class RuleTexts {

	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	public static String resolve(String identifier) {

		switch (identifier) {
		case "OrRule":
			return TEXTS.orRule();
		case "AndRule":
			return TEXTS.andRule();
		case "XorRule":
			return TEXTS.xorRule();
		case "NotRule":
			return TEXTS.notRule();
		case "MatchAlwaysRule":
			return TEXTS.matchAlwaysRule();
		case "SenderInOuRule":
			return TEXTS.senderInOURule();
		case "SenderInGroupRule":
			return TEXTS.senderInGroupRule();
		case "AddSignatureAction":
			return TEXTS.signatureAction();
		case "RecipientIsExternalRule":
			return TEXTS.recipientIsExternalRule();
		case "RecipientIsInternalRule":
			return TEXTS.recipientIsInternalRule();
		case "SendDateIsBefore":
			return TEXTS.sendDateIsBeforeRule();
		case "SendDateIsAfter":
			return TEXTS.sendDateIsAfterRule();
		default:
			return identifier;
		}
	}

}
