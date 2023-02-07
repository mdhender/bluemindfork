package net.bluemind.tests.extensions;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;

public class WithEsExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Logger logger = LoggerFactory.getLogger(WithVertxExtension.class);

	@Override
	public void beforeEach(ExtensionContext arg0) throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
	}

	@Override
	public void afterEach(ExtensionContext arg0) throws Exception {
		ElasticsearchTestHelper.getInstance().afterTest();
	}

}
