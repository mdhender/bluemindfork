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
package net.bluemind.system.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UpdateHistory {

	public List<FromTo> updates;

	public UpdateHistory() {
		this.updates = new ArrayList<>();
	}

	public List<FromTo> getUpdates() {
		return updates;
	}

	public void setUpdates(List<FromTo> updates) {
		this.updates = updates;
	}

	public void add(String from, String to) {
		updates.add(new FromTo(new Date(), from, to));
	}

	public static class FromTo {
		Date date;
		String from;
		String to;

		public FromTo() {

		}

		public FromTo(Date date, String from, String to) {
			this.date = date;
			this.from = from;
			this.to = to;
		}

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

	}

}
