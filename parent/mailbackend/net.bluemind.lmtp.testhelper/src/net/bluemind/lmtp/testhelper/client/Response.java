/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.client;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class Response {

	private static final Logger logger = LoggerFactory.getLogger(Response.class);

	public static class ResponseBuilder {
		List<String> responses = new LinkedList<>();

		public void part(String s) {
			logger.debug("Add {} to {}", s, this);
			this.responses.add(s);
		}

		public Response build(String last) {
			part(last);
			return new Response(ImmutableList.copyOf(responses));
		}
	}

	private final List<String> respParts;

	private Response(ImmutableList<String> copyOf) {
		this.respParts = copyOf;
	}

	public List<String> parts() {
		return respParts;
	}

	public static ResponseBuilder builder() {
		return new ResponseBuilder();
	}

}
