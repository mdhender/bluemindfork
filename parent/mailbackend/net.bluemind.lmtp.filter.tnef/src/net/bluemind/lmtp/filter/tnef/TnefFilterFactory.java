/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.lmtp.filter.tnef;

import net.bluemind.delivery.lmtp.filters.ILmtpFilterFactory;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;

public class TnefFilterFactory implements ILmtpFilterFactory {

	private TnefFilter filter;

	public TnefFilterFactory() {
		this.filter = new TnefFilter();
	}

	@Override
	public int getPriority() {
		return Integer.MAX_VALUE;
	}

	@Override
	public IMessageFilter getEngine() {
		return filter;
	}

}
