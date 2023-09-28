package net.bluemind.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		InstallationId.init();

		try {
			Path etcbm = Token.TOKEN_PATH.getParent();
			WatchService watchService = FileSystems.getDefault().newWatchService(); // NOSONAR: handled in
																					// Token.stopWatcher
			etcbm.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE);
			Token.startWatcher(watchService);
		} catch (IOException ignored) {
			// ignored
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Token.stopWatcher();
		Activator.context = null;
	}

}
