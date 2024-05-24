package net.bluemind.system.ldap.export;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.ldap.export.enhancer.IEntityEnhancer;

public class Activator implements BundleActivator {
	private static final List<IEntityEnhancer> entityEnhancerHooks = loadEntityEnhancerHooks();

	/**
	 * USE WITH CAUTION
	 * 
	 * May be used for specific purpose (upgrade from version with no .internal
	 * domain UID...) to keep DN as on previous LDAP export
	 */
	private static final String DOMAIN_UID_MAPPING_FILE = "/etc/bm/bm-ldap-export.domainNameMap";
	private static final Properties domainNameMapping = initDomainNameMapping();

	@Override
	public void start(BundleContext context) throws Exception {
		// Nothing to do
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// Nothing to do
	}

	public static String getDomainNameMappedValue(String domainUid) {
		return domainNameMapping.getProperty(domainUid, domainUid);
	}

	private static List<IEntityEnhancer> loadEntityEnhancerHooks() {
		RunnableExtensionLoader<IEntityEnhancer> loader = new RunnableExtensionLoader<IEntityEnhancer>();
		return loader.loadExtensionsWithPriority("net.bluemind.system.ldap.export", "entityenhancer", "hook", "impl");
	}

	public static List<IEntityEnhancer> getEntityEnhancerHooks() {
		return entityEnhancerHooks;
	}

	private static Properties initDomainNameMapping() {
		File f = new File(DOMAIN_UID_MAPPING_FILE);
		if (!f.exists()) {
			return new Properties();
		}

		try (var in = Files.newInputStream(f.toPath())) {
			Properties p = new Properties();
			p.load(in);

			return p;
		} catch (Exception e) {
			return new Properties();
		}
	}
}
