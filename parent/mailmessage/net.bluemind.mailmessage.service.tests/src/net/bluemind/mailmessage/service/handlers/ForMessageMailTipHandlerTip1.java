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
package net.bluemind.mailmessage.service.handlers;

import java.util.Arrays;
import java.util.List;

import net.bluemind.mailmessage.api.IMailTipEvaluation;
import net.bluemind.mailmessage.api.MessageContext;

public class ForMessageMailTipHandlerTip1 implements IMailTipEvaluation {

	@Override
	public String mailtipType() {
		return "TestTip1";
	}

	@Override
	public List<EvaluationResult> evaluate(String domainUid, MessageContext messageContext) {
		return Arrays.asList(EvaluationResult.matchesForMessage("ForMessageMailTipHandlerTip1"));
	}

}
