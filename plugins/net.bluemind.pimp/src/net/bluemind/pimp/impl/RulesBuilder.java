package net.bluemind.pimp.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

public final class RulesBuilder {

	public Rule[] build() {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.readValue(openRulesJson(), new TypeReference<Rule[]>() {
			});
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

	}

	private InputStream openRulesJson() throws FileNotFoundException {
		File f = new File("/etc/bm/local/rules.json");
		if (f.exists()) {
			return new FileInputStream(f);
		} else {
			return getClass().getClassLoader().getResourceAsStream("data/rules.json");
		}
	}

}
