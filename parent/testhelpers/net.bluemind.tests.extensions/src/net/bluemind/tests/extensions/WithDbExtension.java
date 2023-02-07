package net.bluemind.tests.extensions;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import net.bluemind.core.jdbc.JdbcTestHelper;

public class WithDbExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(ExtensionContext arg0) throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
	}

	@Override
	public void afterEach(ExtensionContext arg0) throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

}
