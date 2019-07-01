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
package net.bluemind.dataprotect.api;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.VersionInfo;

/**
 * This class represents a generation of protected Blue Mind data.
 * 
 * A {@link DataProtectGeneration} records when your data was protected, the
 * Blue Mind version and all the {@link ProtectedPart}s of the Blue Mind
 * infrastructure at this point in time.
 */
@BMApi(version = "3")
public class DataProtectGeneration {

	public int id;
	public Date protectionTime;
	public VersionInfo blueMind;
	public boolean withWarnings;
	public boolean withErrors;

	public List<PartGeneration> parts = new LinkedList<>();

	public String toString() {
		return "[BlueMind " + blueMind + " @ " + protectionTime + "]";
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object o) {
		return o instanceof DataProtectGeneration && ((DataProtectGeneration) o).id == id;
	}

	public boolean valid() {
		if (parts.isEmpty()) {
			return false;
		}
		return parts.stream().noneMatch(part -> part.valid != GenerationStatus.VALID);
	}
}
