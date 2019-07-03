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
package net.bluemind.lmtp.backend;

/**
 * Holds informations about were in email was delivered, taking filters into
 * account.
 * 
 */
public class DeliveredVersion {

	private final String mbox;

	public DeliveredVersion(String mbox) {
		this.mbox = mbox;
	}

	public String getMbox() {
		return mbox;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mbox == null) ? 0 : mbox.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeliveredVersion other = (DeliveredVersion) obj;
		if (mbox == null) {
			if (other.mbox != null)
				return false;
		} else if (!mbox.equals(other.mbox))
			return false;
		return true;
	}

}
