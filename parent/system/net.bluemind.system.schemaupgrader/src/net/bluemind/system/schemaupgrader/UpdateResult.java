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
package net.bluemind.system.schemaupgrader;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class UpdateResult {

	public final Result result;
	public final Set<UpdateAction> actions;

	public UpdateResult(Result result, Set<UpdateAction> actions) {
		this.result = result;
		this.actions = ImmutableSet.<UpdateAction>builder().addAll(actions).build();
	}

	public UpdateResult(Result result) {
		this(result, Collections.emptySet());
	}

	public enum Result {
		OK, FAILED, NOOP
	}

	public static UpdateResult ok() {
		return new UpdateResult(Result.OK);
	}

	public static UpdateResult failed() {
		return new UpdateResult(Result.FAILED);
	}

	public static UpdateResult noop() {
		return new UpdateResult(Result.NOOP);
	}

	@Override
	public int hashCode() {
		return 31 + ((this.result == null) ? 0 : this.result.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj || obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateResult other = (UpdateResult) obj;
		return result == other.result;
	}

}
