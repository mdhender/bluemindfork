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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import net.bluemind.imap.impl.IMAPResponse;

public abstract class AbstractUIDSearchCommand extends Command<Collection<Integer>> {

	@Override
	public final void responseReceived(List<IMAPResponse> rs) {
		data = Collections.emptyList();

		if (isOk(rs)) {
			String uidString = null;
			Iterator<IMAPResponse> it = rs.iterator();
			int len = rs.size() - 1;
			for (int j = 0; j < len; j++) {
				String resp = it.next().getPayload();
				if (resp.startsWith("* SEARCH ")) {
					uidString = resp;
					break;
				}
			}

			if (uidString != null) {
				// 9 => '* SEARCH '.length
				StringTokenizer st = new StringTokenizer(uidString.substring(9), " ", false);
				List<Integer> result = new LinkedList<>();
				while (st.hasMoreTokens()) {
					result.add(Integer.parseInt(st.nextToken()));
				}

				data = result;
			}
		}
	}

}
