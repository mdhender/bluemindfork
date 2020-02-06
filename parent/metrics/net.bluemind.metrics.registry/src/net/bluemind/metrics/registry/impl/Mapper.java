package net.bluemind.metrics.registry.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Mapper {
	private static final ObjectMapper mapper = new ObjectMapper();

	private Mapper() {
	}

	public static ObjectMapper get() {
		return mapper;
	}
}
