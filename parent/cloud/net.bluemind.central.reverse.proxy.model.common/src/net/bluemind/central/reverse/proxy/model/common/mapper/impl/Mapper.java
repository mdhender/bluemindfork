package net.bluemind.central.reverse.proxy.model.common.mapper.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public class Mapper {

	public static final ObjectMapper mapper = create();

	private static ObjectMapper create() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
		return objectMapper;
	}
}
