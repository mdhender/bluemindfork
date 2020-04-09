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
package net.bluemind.core.container.model;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ErrorCode;

@BMApi(version = "3")
public class ContainerUpdatesResult {

	public List<String> added = new ArrayList<>();
	public List<String> updated = new ArrayList<>();
	public List<String> removed = new ArrayList<>();
	public List<String> unhandled = new ArrayList<>();

	public List<InError> errors;

	public long version;

	@BMApi(version = "3")
	public static class InError {
		public String message;
		public String uid;
		public ErrorCode errorCode;

		public static InError create(String message, ErrorCode errorCode, String uid) {
			InError ret = new InError();
			ret.message = message;
			ret.uid = uid;
			ret.errorCode = errorCode;
			return ret;
		}
	}

	public int total() {
		return synced() + unhandled.size();
	}

	public int synced() {
		return added.size() + updated.size() + removed.size();
	}
}
