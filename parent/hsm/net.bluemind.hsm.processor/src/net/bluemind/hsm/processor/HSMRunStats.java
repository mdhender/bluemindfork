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
package net.bluemind.hsm.processor;

import java.util.concurrent.atomic.AtomicInteger;

public class HSMRunStats {

	private AtomicInteger moved = new AtomicInteger(0);
	private AtomicInteger pmatch = new AtomicInteger(0);
	private AtomicInteger pmismatch = new AtomicInteger(0);

	public void mailMoved() {
		moved.incrementAndGet();
	}

	public void policyMatch() {
		pmatch.incrementAndGet();
	}

	public int getMovedMailsCount() {
		return moved.intValue();
	}

	public int getMatchedMailsCount() {
		return pmatch.intValue();
	}

	public int getMismatchCount() {
		return pmismatch.intValue();
	}

	public void policyMismatch() {
		pmismatch.incrementAndGet();
	}

}
