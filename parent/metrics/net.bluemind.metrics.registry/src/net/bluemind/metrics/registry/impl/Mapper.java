package net.bluemind.metrics.registry.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Mapper {
	static ObjectMapper mapper = new ObjectMapper();
	
	public static ObjectMapper get() {
		return mapper;
	}
}
