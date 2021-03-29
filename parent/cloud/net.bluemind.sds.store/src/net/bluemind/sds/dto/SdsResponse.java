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
package net.bluemind.sds.dto;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class SdsResponse {

	public static final SdsResponse UNTAGGED_OK = new SdsResponse() {
		@Override
		public SdsResponse withTags(Map<String, String> t) {
			return this;
		}
	};

	public SdsError error;
	private Map<String, String> tags = Collections.emptyMap();
	private long size = 0;

	public boolean succeeded() {
		return error == null;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass()).add("success", succeeded()).add("error", error).toString();
	}

	public SdsResponse withTags(Map<String, String> t) {
		this.tags = t;
		return this;
	}

	public SdsResponse withSize(long size) {
		this.size = size;
		return this;
	}

	@JsonProperty("tags")
	public Map<String, String> tags() {
		return tags;
	}

	@JsonProperty("size")
	public long size() {
		return size;
	}

}
