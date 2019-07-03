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

public final class TaggedResult {

	private boolean ok;
	private String[] output;

	public TaggedResult() {
	}

	public boolean isOk() {
		return ok;
	}

	public String[] getOutput() {
		return output;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public void setOutput(String[] output) {
		this.output = output;
	}

}
