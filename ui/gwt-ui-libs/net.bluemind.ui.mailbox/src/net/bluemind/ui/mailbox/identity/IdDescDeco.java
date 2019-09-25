/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.ui.mailbox.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.bluemind.mailbox.identity.api.IdentityDescription;

/**
 * Decorates {@link IdentityDescription} in order to allow uniqueness (add
 * {@link #equals(Object)} and {@link #hashCode()} methods).
 */
public class IdDescDeco {
	private IdentityDescription identityDescription;

	public IdDescDeco(IdentityDescription identityDescription) {
		this.identityDescription = identityDescription;
	}

	public static List<IdDescDeco> fromIdentityDescriptions(
			final Collection<IdentityDescription> identityDescriptions) {
		final List<IdDescDeco> result = new ArrayList<IdDescDeco>(identityDescriptions.size());
		for (final IdentityDescription identityDescription : identityDescriptions) {
			result.add(new IdDescDeco(identityDescription));
		}
		return result;
	}

	public static List<IdentityDescription> toIdentityDescriptions(final Collection<IdDescDeco> idDescDecos) {
		final List<IdentityDescription> result = new ArrayList<IdentityDescription>(idDescDecos.size());
		for (final IdDescDeco idDescDeco : idDescDecos) {
			result.add(idDescDeco.identityDescription);
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.identityDescription.displayname == null) ? 0
				: this.identityDescription.displayname.hashCode());
		result = prime * result
				+ ((this.identityDescription.email == null) ? 0 : this.identityDescription.email.hashCode());
		result = prime * result + ((this.identityDescription.id == null) ? 0 : this.identityDescription.id.hashCode());
		result = prime * result
				+ ((this.identityDescription.isDefault == null) ? 0 : this.identityDescription.isDefault.hashCode());
		result = prime * result
				+ ((this.identityDescription.mbox == null) ? 0 : this.identityDescription.mbox.hashCode());
		result = prime * result
				+ ((this.identityDescription.mboxName == null) ? 0 : this.identityDescription.mboxName.hashCode());
		result = prime * result
				+ ((this.identityDescription.name == null) ? 0 : this.identityDescription.name.hashCode());
		result = prime * result
				+ ((this.identityDescription.signature == null) ? 0 : this.identityDescription.signature.hashCode());
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
		IdentityDescription other = (IdentityDescription) ((IdDescDeco) obj).identityDescription;
		if (this.identityDescription.displayname == null) {
			if (other.displayname != null)
				return false;
		} else if (!this.identityDescription.displayname.equals(other.displayname))
			return false;
		if (this.identityDescription.email == null) {
			if (other.email != null)
				return false;
		} else if (!this.identityDescription.email.equals(other.email))
			return false;
		if (this.identityDescription.id == null) {
			if (other.id != null)
				return false;
		} else if (!this.identityDescription.id.equals(other.id))
			return false;
		if (this.identityDescription.isDefault == null) {
			if (other.isDefault != null)
				return false;
		} else if (!this.identityDescription.isDefault.equals(other.isDefault))
			return false;
		if (this.identityDescription.mbox == null) {
			if (other.mbox != null)
				return false;
		} else if (!this.identityDescription.mbox.equals(other.mbox))
			return false;
		if (this.identityDescription.mboxName == null) {
			if (other.mboxName != null)
				return false;
		} else if (!this.identityDescription.mboxName.equals(other.mboxName))
			return false;
		if (this.identityDescription.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.identityDescription.name.equals(other.name))
			return false;
		if (this.identityDescription.signature == null) {
			if (other.signature != null)
				return false;
		} else if (!this.identityDescription.signature.equals(other.signature))
			return false;
		return true;
	}
}