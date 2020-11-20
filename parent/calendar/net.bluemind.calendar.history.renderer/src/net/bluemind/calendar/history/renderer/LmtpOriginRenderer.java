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
package net.bluemind.calendar.history.renderer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.service.IChangelogOriginRenderer;
import net.bluemind.core.context.SecurityContext;

public class LmtpOriginRenderer implements IChangelogOriginRenderer {

	private static final Pattern with_from_to = Pattern.compile("bm-lmtpd_from_(.*)_to_(.*)");
	private static final Pattern with_to = Pattern.compile("bm-lmtpd_to_(.*)");

	/**
	 * 
	 * The value handled here is set by AbstractLmtpHandler
	 *
	 */
	@Override
	public ItemChangelog render(SecurityContext sc, ItemChangelog raw) {

		for (ItemChangeLogEntry cle : raw.entries) {
			if (cle.origin != null) {
				cle.origin = processOrigin(cle.origin);
			}
		}

		return raw;
	}

	@VisibleForTesting
	public String processOrigin(String origin) {
		String ret = origin;
		if (origin.startsWith("bm-lmtpd")) {
			if ("bm-lmtpd".equals(origin)) {
				ret = "BlueMind LMTPd";
			} else {
				Matcher matcher = with_from_to.matcher(origin);
				if (matcher.matches()) {
					String from = matcher.group(1);
					String to = matcher.group(2);
					ret = from + " => " + to + " (LMTPd)";
				} else {
					Matcher toOnly = with_to.matcher(origin);
					if (toOnly.matches()) {
						ret = "BlueMind LMTPd => " + toOnly.group(1);
					}
				}
			}
		}
		return ret;
	}

}
