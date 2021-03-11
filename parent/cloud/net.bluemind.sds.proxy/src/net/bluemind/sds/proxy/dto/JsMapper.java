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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.sds.store.SdsException;

public class JsMapper {

	private static final Logger logger = LoggerFactory.getLogger(JsMapper.class);
	private static final ObjectMapper mapper = initMapper();

	public static final ObjectMapper get() {
		return mapper;
	}

	private JsMapper() {

	}

	private static ObjectMapper initMapper() {
		ObjectMapper om = new ObjectMapper();
		logger.debug("OM is {}", om);
		return om;
	}

	public static <T> T readValue(String s, Class<T> type) {
		try {
			return get().readValue(s, type);
		} catch (JsonProcessingException e) {
			throw new SdsException(e);
		}
	}

	public static <T> String writeValueAsString(T obj) {
		try {
			return get().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new SdsException(e);
		}
	}

}
