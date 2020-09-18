package net.bluemind.metrics.registry.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public class Mapper {
	private static final ObjectMapper INST = createMapper();

	private static ObjectMapper createMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
		return om;
	}

	private Mapper() {
	}

	public static ObjectMapper get() {
		return INST;
	}
}
