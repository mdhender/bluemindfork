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
package net.bluemind.mailmessage.api;

import java.util.List;

import net.bluemind.mailflow.common.api.Recipient;
import net.bluemind.mailmessage.api.MessageContext;

public interface IMailTipEvaluation {

	public String mailtipType();

	public List<EvaluationResult> evaluate(String domainUid, MessageContext messageContext);

	public static class EvaluationResult {
		public final Recipient recipient;
		public final String value;

		private EvaluationResult(Recipient recipient, String value) {
			this.recipient = recipient;
			this.value = value;
		}

		public static EvaluationResult matchesForRecipient(Recipient recipient, String value) {
			return new EvaluationResult(recipient, value);
		}

		public static EvaluationResult matchesForMessage(String value) {
			return new EvaluationResult(null, value);
		}
	}
}
