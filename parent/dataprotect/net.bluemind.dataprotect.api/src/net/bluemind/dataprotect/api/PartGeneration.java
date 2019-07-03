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

import net.bluemind.core.api.BMApi;

/**
 * A generation is a point-in-time snapshot, de-duplicated and compressed of
 * some data.
 * 
 * A generation is referenced by a {@link ProtectedPart} which are tied together
 * in a {@link DataProtectGeneration} to represent a complete backup.
 */
@BMApi(version = "3")
public class PartGeneration {

	public int id;
	public int generationId;
	public Date begin;
	public Date end;
	public long size;
	public String tag;
	public String server;
	public boolean withWarnings;
	public boolean withErrors;
	public GenerationStatus valid;
	public String datatype;

	@Override
	public String toString() {
		return "[id: " + id + ", taken at " + end + ", size: " + size + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		PartGeneration other = (PartGeneration) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public void validate() {
		if (withErrors) {
			valid = GenerationStatus.INVALID;
		} else if (withWarnings) {
			valid = GenerationStatus.UNKNOWN;
		} else {
			valid = GenerationStatus.VALID;
		}
	}

	public static PartGeneration create(int id, String server, String tag) {
		PartGeneration pg = new PartGeneration();
		pg.id = id;
		pg.server = server;
		pg.tag = tag;
		return pg;
	}

}
