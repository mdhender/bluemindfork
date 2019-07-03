package net.bluemind.elasticsearch.initializer.tests;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

import net.bluemind.elasticsearch.initializer.AbstractSchemaInitializer;

public class TestInitializer extends AbstractSchemaInitializer {

	public static String indexName;

	@Override
	public String getTag() {
		return "tag/test";
	}

	@Override
	public String getSchemaAsString() {

		byte[] content = null;
		try (InputStream schemaStream = TestInitializer.class.getResourceAsStream("test-schema.json");) {
			content = ByteStreams.toByteArray(schemaStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new String(content);
	}

	@Override
	public String getType() {
		return "test";
	}

	@Override
	protected String getIndexName() {
		return indexName;
	}

}
