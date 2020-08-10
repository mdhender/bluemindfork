/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.milter.action.misc.updatesubject;

import java.util.Map;

import com.google.common.base.Strings;

import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;

public class UpdateSubjectAction implements MilterAction {
	private static final String identifier = "UpdateSubjectAction";

	public static class UpdateSubjectActionFactory implements MilterActionsFactory {
		@Override
		public MilterAction create() {
			return new UpdateSubjectAction();
		}
	}

	@Override
	public String identifier() {
		return identifier;
	}

	@Override
	public String description() {
		return "Update mail subject";
	}

	@Override
	public void execute(UpdatedMailMessage modifier, Map<String, String> configuration,
			Map<String, String> evaluationData, IClientContext mailflowContext) {
		String newSubject = (Strings.isNullOrEmpty(configuration.get("subjectPrefix")) ? ""
				: configuration.get("subjectPrefix"))
				+ (Strings.isNullOrEmpty(modifier.getMessage().getSubject()) ? "" : modifier.getMessage().getSubject())
				+ (Strings.isNullOrEmpty(configuration.get("subjectSuffix")) ? "" : configuration.get("subjectSuffix"));

		modifier.removeHeader("Subject");
		modifier.addHeader("Subject", newSubject, identifier());
	}
}
