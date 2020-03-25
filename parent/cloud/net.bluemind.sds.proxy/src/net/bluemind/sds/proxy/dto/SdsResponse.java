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
package net.bluemind.sds.proxy.dto;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class SdsResponse {

	public SdsError error;
	private Map<String, String> tags = Collections.emptyMap();

	public boolean succeeded() {
		return error == null;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass()).add("success", succeeded()).add("error", error).toString();
	}

	public void withTags(Map<String, String> t) {
		this.tags = t;
	}

	@JsonProperty("tags")
	public Map<String, String> tags() {
		return tags;
	}

}
