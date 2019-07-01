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
package net.bluemind.imap.command;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.QuotaInfo;
import net.bluemind.imap.impl.IMAPResponse;

public class QuotaRootCommand extends SimpleCommand<QuotaInfo> {

	/**
	 * UTF-7 transformation is not applied
	 * 
	 * @param mailbox
	 */
	public QuotaRootCommand(String mailbox) {
		super("GETQUOTAROOT " + mailbox);
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = new QuotaInfo();
		if (isOk(rs)) {
			Pattern p = Pattern.compile("\\* QUOTA .* \\(STORAGE ");
			for (IMAPResponse imapr : rs) {
				if (logger.isDebugEnabled()) {
					logger.debug("Payload: " + imapr.getPayload());
				}
				Matcher m = p.matcher(imapr.getPayload());
				if (m.find()) {
					String rep = m.replaceAll("").replaceAll("\\)", "");
					String[] tab = rep.split(" ");
					if (tab.length == 2) {
						data = new QuotaInfo(Integer.parseInt(tab[0]), Integer.parseInt(tab[1]));
					}
				}
			}
		}
	}

}
