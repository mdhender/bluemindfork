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
package net.bluemind.mailmessage.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailflow.common.api.Recipient;
import net.bluemind.mailmessage.api.IMailTip;
import net.bluemind.mailmessage.api.IMailTipEvaluation;
import net.bluemind.mailmessage.api.MailTip;
import net.bluemind.mailmessage.api.MailTipContext;
import net.bluemind.mailmessage.api.MailTipFilter;
import net.bluemind.mailmessage.api.MailTipFilter.FilterType;
import net.bluemind.mailmessage.api.MailTips;
import net.bluemind.mailmessage.api.IMailTipEvaluation.EvaluationResult;
import net.bluemind.mailmessage.service.Activator;

public class MailMessageService implements IMailTip {

	private String domainUid;

	public MailMessageService(BmContext context, String domainUid) {
		this.domainUid = domainUid;
	}

	@Override
	public List<MailTips> getMailTips(MailTipContext mailtipContext) throws ServerFault {
		List<MailTips> matchingTips = new ArrayList<>();

		for (Entry<String, List<IMailTipEvaluation>> handlers : Activator.mailtipHandlers.entrySet()) {
			if (handles(handlers.getKey(), mailtipContext.filter)) {
				for (IMailTipEvaluation handler : handlers.getValue()) {
					matchingTips
							.addAll(handler.evaluate(domainUid, mailtipContext.messageContext).stream().map((result) -> {
								return createTip(handler, result);
							}).collect(Collectors.toList()));
				}
			}
		}
		return merge(matchingTips);

	}

	private MailTips createTip(IMailTipEvaluation handler, EvaluationResult result) {
		MailTips tips = new MailTips();
		tips.forRecipient = result.recipient;
		MailTip mailTip = new MailTip();
		mailTip.mailtipType = handler.mailtipType();
		mailTip.value = result.value;
		tips.matchingTips = Arrays.asList(mailTip);
		return tips;
	}

	private List<MailTips> merge(List<MailTips> matchingTips) {
		List<MailTips> mergedList = new ArrayList<>();
		Map<Recipient, List<MailTip>> merged = new HashMap<>();

		matchingTips.forEach(
				tip -> merged.computeIfAbsent(tip.forRecipient, k -> new ArrayList<>()).addAll(tip.matchingTips));

		for (Entry<Recipient, List<MailTip>> m : merged.entrySet()) {
			MailTips tip = new MailTips();
			tip.forRecipient = m.getKey();
			tip.matchingTips = m.getValue();
			mergedList.add(tip);
		}

		return mergedList;
	}

	private boolean handles(String type, MailTipFilter filter) {
		if (filter == null) {
			return true;
		}
		boolean contains = filter.mailTips.contains(type);
		return filter.filterType == FilterType.INCLUDE ? contains : !contains;
	}

}
