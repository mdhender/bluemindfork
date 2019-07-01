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
package net.bluemind.imap;

import java.io.Serializable;

@SuppressWarnings("serial")
public class QuotaInfo implements Serializable {

	private boolean enable;
	private int usage;
	private int limit;

	public QuotaInfo() {
		this.enable = false;
		this.usage = 0;
		this.limit = 0;
	}

	/**
	 * @param usages
	 *            current KiB
	 * @param limites
	 *            max KiB
	 */
	public QuotaInfo(int usages, int limites) {
		this.enable = true;
		this.usage = usages;
		this.limit = limites;
	}

	public boolean isEnable() {
		return enable;
	}

	/**
	 * Usage in KiB
	 * 
	 * @return
	 */
	public int getUsage() {
		return usage;
	}

	/**
	 * Limit in KiB
	 * 
	 * @return
	 */
	public int getLimit() {
		return limit;
	}
}
