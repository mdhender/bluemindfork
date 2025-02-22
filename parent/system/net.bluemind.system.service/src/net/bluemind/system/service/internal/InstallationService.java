/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.service.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.api.CloneDefaults;
import net.bluemind.core.backup.continuous.api.InstallationWriteLeader;
import net.bluemind.core.bo.report.provider.HostReportProvider;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.DbSchemaService;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.LoggingTaskMonitor;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.CloneConfiguration;
import net.bluemind.system.api.CustomLogo;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.PublicInfos;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.SubscriptionInformations.Kind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.api.UpgradeStatus;
import net.bluemind.system.helper.ArchiveHelper;
import net.bluemind.system.helper.distrib.OsVersionDetectionFactory;
import net.bluemind.system.helper.distrib.list.Distribution;
import net.bluemind.system.persistence.UpgraderStore;
import net.bluemind.system.schemaupgrader.ComponentVersion;
import net.bluemind.system.schemaupgrader.ComponentVersionExtensionPoint;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.service.clone.CloneSupport;
import net.bluemind.system.service.helper.BlockingHotupgradesCheck;
import net.bluemind.system.state.StateContext;
import net.bluemind.system.subscriptionprovider.SubscriptionProviders;
import net.bluemind.systemcheck.UpgradeCheck;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class InstallationService implements IInstallation {

	private static final Logger logger = LoggerFactory.getLogger(InstallationService.class);
	private BmContext context;
	private UpgraderStore schemaVersionStore;

	public InstallationService(BmContext context) {
		this.context = context;
		this.schemaVersionStore = new UpgraderStore(context.getDataSource());
	}

	@Override
	public TaskRef upgrade() throws ServerFault {
		checkPermissions();

		return context.provider().instance(ITasksManager.class).run(new InstallationUpgradeTask(context, getVersion()));
	}

	private void checkPermissions() {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can do upgrade", ErrorCode.NOT_GLOBAL_ADMIN);
		}

		SystemState coreState = StateContext.getState();
		if (!(coreState == SystemState.CORE_STATE_MAINTENANCE || coreState == SystemState.CORE_STATE_RUNNING
				|| coreState == SystemState.CORE_STATE_UPGRADE)) {
			throw new ServerFault("Upgrade is not available in state " + coreState);
		}
	}

	@Override
	public TaskRef postinst() {
		checkPermissions();

		return context.provider().instance(ITasksManager.class).run(new PostInstTask());
	}

	@Override
	public void demoteLeader() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Operation demoteLeader is only permitted for admin0", ErrorCode.PERMISSION_DENIED);
		}
		logger.info("We should STEP-DOWN from leader position, current state is {}", StateContext.getState());
		// LeaderStateListener will run the STEP-DOWN actions
		StateContext.setState("core.demote.start");

	}

	@Override
	public void promoteLeader() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Operation proteLeader is only permitted for admin0", ErrorCode.PERMISSION_DENIED);
		}
		if (StateContext.getState() != SystemState.CORE_STATE_CLONING) {
			throw new ServerFault("Promote leader can only be called in cloning state");
		}
		// enable backup store
		boolean del = new File(CloneDefaults.MARKER_FILE_PATH).delete();
		logger.info("continuous clone marker deletion: {}", del);
		InstallationWriteLeader leadership = cloneSupport().leadership();
		leadership.applyForLeadership();
		int retry = 0;
		while (!leadership.isLeader() && ++retry < 20) {
			logger.info("Waiting for election as leader with {} (retry {})", leadership, retry);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		logger.info("Elected as leader with {} (leader: {})", leadership, leadership.isLeader());

		// write topo with backup store enabled
		cloneSupport().resume();
		IServer srvApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> all = srvApi.allComplete();
		logger.info("Write my topology ({} server(s)) in backup store to step-up", all.size());
		all.forEach(srv -> srvApi.update(srv.uid, srv.value));

		StateContext.setState("core.cloning.end");

	}

	@Override
	public TaskRef clone(CloneConfiguration conf) {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Operation is only permitted for admin0", ErrorCode.PERMISSION_DENIED);
		}
		CloneSupport impl = cloneSupport();
		InstallationId.reload();

		logger.info("Set sysconf overrides {}...", conf.sysconfOverride);
		context.provider().instance(ISystemConfiguration.class).updateMutableValues(conf.sysconfOverride);

		logger.info("[{}] Clone impl is {}", InstallationId.getIdentifier(), impl);
		IServerTask tsk = impl.create(conf, context.provider(), conf.sysconfOverride);
		IServerTask wrapped = new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				tsk.execute(monitor);
				StateContext.setState("core.cloning.end");
				repairHollow(context.provider());
			}
		};

		return context.provider().instance(ITasksManager.class).run(logger, wrapped);
	}

	private CloneSupport cloneSupport() {
		RunnableExtensionLoader<CloneSupport> clones = new RunnableExtensionLoader<>();
		List<CloneSupport> loaded = clones.loadExtensions("net.bluemind.system.service", "clone_support",
				"clone_support", "impl");
		if (loaded.isEmpty()) {
			throw new ServerFault("No implementors of clone_support");
		}
		return loaded.get(0);
	}

	private void repairHollow(IServiceProvider provider) {
		IDomains domains = provider.instance(IDomains.class);
		domains.all().stream() //
				.filter(domain -> !domain.uid.equals("global.virt")) //
				.forEach(domain -> repairHollow(provider, domain.uid));
	}

	private void repairHollow(IServiceProvider provider, String domainUid) {
		IDirEntryMaintenance demService = provider.instance(IDirEntryMaintenance.class, domainUid, domainUid);
		Set<String> filteredOps = Collections.singleton("hollow.directory");
		logger.info("Starting hollow repair task for {}", domainUid);
		TaskRef ref = demService.repair(RepairConfig.create(filteredOps, false, true, true));

		String log = TaskUtils.logStreamWait(provider, ref);
		logger.debug("Hollow repair task log for {}: {}", domainUid, log);
		logger.info("Ending hollow repair task for {}", domainUid);
	}

	@Override
	public TaskRef initialize() throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			protected void run(IServerTaskMonitor monitor) throws Exception {
				initializeSystem(monitor);
			}
		});
	}

	private void initializeSystem(IServerTaskMonitor monitor) {
		monitor.begin(100, "Initializing system...");

		File token = new File("/etc/bm/bm-core.tok");
		if (token.exists() && !context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can initialize", ErrorCode.NOT_GLOBAL_ADMIN);
		}

		File f = new File("/etc/bm/mcast.id");
		if (f.exists()) {
			logger.warn("mcast.id is already present, we create a new installation on an existing one !");
		}
		File clone = new File(CloneDefaults.TARGET_MCAST_ID);
		boolean cloning = clone.exists();
		try {
			if (cloning) {
				StateContext.setState("core.cloning.start");
				logger.info("Using mcast.id.clone for installation");
				Files.copy(clone.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				StateContext.setState("core.upgrade.start");
				Files.write(f.toPath(), UUID.randomUUID().toString().getBytes());
			}
			InstallationId.reload();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during installation initialisation : " + e.getMessage());
		}

		monitor.progress(10, "Created mcast...");

		try {
			JdbcActivator.getInstance().restartDataSource();
		} catch (Exception e) {
			logger.error("Error during database pool restarting", e);
			throw new ServerFault("Error during database pool restarting: " + e.getMessage());
		}
		DbSchemaService dbSchemaService = DbSchemaService.getService(JdbcActivator.getInstance().getDataSource(), true);
		dbSchemaService.initialize();

		UpgraderStore store = new UpgraderStore(JdbcActivator.getInstance().getDataSource());

		JdbcAbstractStore.doOrFail(() -> {
			for (ComponentVersion cp : ComponentVersionExtensionPoint.getComponentsVersion()) {
				store.updateComponentVersion(cp.identifier, cp.version);
			}
			return null;
		});
		monitor.progress(50, "Created database schema...");

		// Create Installation container
		ContainerStore cs = new ContainerStore(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext(),
				JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);

		try {
			cs.create(Container.create(InstallationId.getIdentifier(), "installation", "installation",
					SecurityContext.SYSTEM.getSubject(), true));

			// Create domains container
			cs.create(Container.create(DomainsContainerIdentifier.getIdentifier(), "domains", "domains",
					SecurityContext.SYSTEM.getSubject(), true));

			// create installation resources container
			cs.create(Container.create("installation_resources", "installation_resources", "installation_resources",
					SecurityContext.SYSTEM.getSubject(), true));

		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration confService = provider.instance(ISystemConfiguration.class);
		Map<String, String> values = new HashMap<>();
		values.put("db_version", BMVersion.getVersion());
		confService.updateMutableValues(values);

		IDomains domains = provider.instance(IDomains.class);
		Domain globalDomain = Domain.create("global.virt", "global.virt", "Global domain", new HashSet<>());
		globalDomain.global = true;
		domains.create("global.virt", globalDomain);

		monitor.progress(40, "Created domain global.virt...");

		IUser userService = provider.instance(IUser.class, "global.virt");
		User admin = new User();
		admin.login = "admin0";
		admin.password = "admin";// NOSONAR
		admin.routing = Mailbox.Routing.none;
		admin.emails = List.of(net.bluemind.core.api.Email.create("admin0@global.virt", true));
		VCard card = new VCard();
		card.identification.name = VCard.Identification.Name.create("admin0", "admin0", null, null, null, null);
		admin.contactInfos = card;
		admin.system = true;
		String uid = "admin0_global.virt";

		userService.create(uid, admin);

		Set<String> roles = new HashSet<>();
		roles.add(SecurityContext.ROLE_SYSTEM);
		roles.add(SecurityContext.ROLE_ADMIN);
		roles.add(BasicRoles.ROLE_SELF_CHANGE_PASSWORD);
		userService.setRoles(uid, roles);

		try {
			registerInstallationDate(provider);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during installation initialisation, Failed to register installation date : "
					+ e.getMessage());
		}

		if (!cloning) {
			StateContext.setState("core.upgrade.end");
		}
		monitor.end(true, "Initialized system", null);
	}

	private void registerInstallationDate(ServerSideServiceProvider provider) throws Exception {
		File ref = new File("/usr/share/bm-core/pkg_plugins")
				.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"))[0];
		BasicFileAttributes attr = Files.readAttributes(ref.toPath(), BasicFileAttributes.class);
		FileTime mTime = attr.lastModifiedTime();
		FileTime cTime = attr.creationTime();
		LocalDate ld = null;
		if (cTime.toMillis() < mTime.toMillis()) {
			ld = LocalDate.from(cTime.toInstant().atZone(ZoneId.systemDefault()));
		} else {
			ld = LocalDate.from(mTime.toInstant().atZone(ZoneId.systemDefault()));
		}
		String installationReleaseDate = DateTimeFormatter.ISO_LOCAL_DATE.format(ld);
		ISystemConfiguration sysConfService = provider.instance(ISystemConfiguration.class);
		Map<String, String> map = new HashMap<>();
		map.put(SysConfKeys.installation_release_date.name(), installationReleaseDate);
		sysConfService.updateMutableValues(map);
	}

	@Override
	public InstallationVersion getVersion() throws ServerFault {

		if (context.getSecurityContext().isAnonymous()) {
			throw new ServerFault("Invalid security context", ErrorCode.PERMISSION_DENIED);
		}

		InstallationVersion ret = new InstallationVersion();

		ret.softwareVersion = BMVersion.getVersion();
		ret.versionName = BMVersion.getVersionName();
		try {
			ret.databaseVersion = systemConfService().getValues().stringValue("db_version");
		} catch (Exception e) {
			logger.info("error retrieving database version : {}", e.getMessage(), e);
		}

		List<ComponentVersion> installedComponents = ComponentVersionExtensionPoint.getComponentsVersion();
		List<ComponentVersion> componentDbVersion = getComponentsVersion();
		boolean upToDate = installedComponents.stream().allMatch(installedComp -> {
			return componentDbVersion.stream().anyMatch(
					c -> c.identifier.equals(installedComp.identifier) && c.version.equals(installedComp.version));
		});
		ret.needsUpgrade = !upToDate;
		componentDbVersion.stream().filter(comp -> comp.identifier.equals(TagDescriptor.bm_core.getTag())).findFirst()
				.ifPresent(cp -> ret.databaseVersion = cp.version);
		return ret;
	}

	private List<ComponentVersion> getComponentsVersion() {
		try {
			return schemaVersionStore.getComponentsVersion();
		} catch (Exception e) {
			logger.info("error retrieving database version : {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public void markSchemaAsUpgraded() throws ServerFault {

		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can do upgrade", ErrorCode.NOT_GLOBAL_ADMIN);
		}

		JdbcAbstractStore.doOrFail(() -> {
			for (ComponentVersion cp : ComponentVersionExtensionPoint.getComponentsVersion()) {
				schemaVersionStore.updateComponentVersion(cp.identifier, cp.version);
			}
			return null;
		});
	}

	@Override
	public SubscriptionInformations getSubscriptionInformations() throws ServerFault {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);

		return SubscriptionProviders.getSubscriptionProvider().loadSubscriptionInformations();
	}

	@Override
	public Kind getSubscriptionKind() throws ServerFault {
		if (context.getSecurityContext().isAnonymous()) {
			throw new ServerFault("Invalid security context", ErrorCode.PERMISSION_DENIED);
		}
		SubscriptionInformations sub = SubscriptionProviders.getSubscriptionProvider().loadSubscriptionInformations();
		return sub.kind;
	}

	@Override
	public Boolean isValidProductionSubscription() throws ServerFault {
		if (context.getSecurityContext().isAnonymous()) {
			throw new ServerFault("Invalid security context", ErrorCode.PERMISSION_DENIED);
		}

		return SubscriptionProviders.getSubscriptionProvider().loadSubscriptionInformations().validProductiveLicense();
	}

	@Override
	public void updateSubscription(String licence) throws ServerFault {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);
		try {
			SubscriptionProviders.getSubscriptionProvider().updateSubscription(context.getDataSource(),
					Base64.getDecoder().decode(licence), OsVersionDetectionFactory.create().detect());
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.NOT_FOUND) {
				String lang = context.getSecurityContext().getLang();
				lang = lang != null ? lang : "en";
				String i18nMsg = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", Locale.of(lang))
						.getString("subscription.providerNotAvailable");
				throw new ServerFault(i18nMsg, ErrorCode.NOT_FOUND);
			} else {
				throw e;
			}
		}
	}

	@Override
	public void updateSubscriptionWithArchive(Stream archive) throws ServerFault {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);

		File archiveFile = new File(ArchiveHelper.SUBSCRIPTION_ARCHIVE_PATH);
		archiveFile.delete();

		ReadStream<Buffer> read = VertxStream.read(archive);
		final AsyncFile aFile = VertxPlatform.getVertx().fileSystem().openBlocking(archiveFile.getAbsolutePath(),
				new OpenOptions());

		try {
			CompletableFuture<Void> v = read.pipeTo(aFile).toCompletionStage().toCompletableFuture();
			read.resume();
			v.get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Subscription archive file cannot be read because: {}", e.getMessage());
			throw new ServerFault(e);
		}

		ArchiveHelper.checkFileSize(archiveFile);
		Distribution serverOs = OsVersionDetectionFactory.create().detect();
		byte[] licence = ArchiveHelper.getSubscriptionFile(archiveFile, serverOs);
		SubscriptionProviders.getSubscriptionProvider().updateSubscription(context.getDataSource(), licence, serverOs);
		logger.info("Subscription archive has been submitted.");
	}

	@Override
	public void updateSubscriptionVersion(String version) {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);

		if (!UpgradeCheck.setupOk(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM),
				new LoggingTaskMonitor(logger, new NullTaskMonitor(), 0), getVersion())) {
			throw new ServerFault(
					String.format("Unable to set upgrade version to: %s - Checks have been failed", version));
		}

		if (BlockingHotupgradesCheck.blockingHotupgrades(context.getSecurityContext()) > 0) {
			throw new ServerFault(String
					.format("Unable to set upgrade version to: %s - Blocking mandatory Hotupgrades found", version));
		}

		SubscriptionProviders.getSubscriptionProvider()
				.updateSubscriptionUrl(OsVersionDetectionFactory.create().detect(), version);
	}

	@Override
	public void removeSubscription() throws ServerFault {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);

		SubscriptionProviders.getSubscriptionProvider().removeSubscription(OsVersionDetectionFactory.create().detect());
	}

	@Override
	public TaskRef resetIndexes() {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can reset indexes", ErrorCode.PERMISSION_DENIED);
		}

		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			protected void run(IServerTaskMonitor monitor) throws Exception {
				ESearchActivator.clearClientCache();
				ESearchActivator.resetIndexes();
			}

		});
	}

	@Override
	public TaskRef resetIndex(String index) {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can reset an index", ErrorCode.PERMISSION_DENIED);
		}

		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			protected void run(IServerTaskMonitor monitor) throws Exception {
				ESearchActivator.clearClientCache();
				ESearchActivator.resetIndex(index);
				;
			}

		});

	}

	@Override
	public void setLogo(byte[] logo) throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Operation is only permitted for admin0", ErrorCode.PERMISSION_DENIED);
		}
		CustomTheme service = new CustomTheme(context, "installation_resources");
		service.setLogo("installation", logo);
	}

	@Override
	public void deleteLogo() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Operation is only permitted for admin0", ErrorCode.PERMISSION_DENIED);
		}
		CustomTheme service = new CustomTheme(context, "installation_resources");
		service.deleteLogo("installation");
	}

	@Override
	public CustomLogo getLogo() throws ServerFault {
		CustomTheme service = new CustomTheme(context, "installation_resources");
		return service.getLogo("installation");
	}

	@Override
	public SystemState getSystemState() throws ServerFault {
		return StateContext.getState();
	}

	@Override
	public void maintenanceMode() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can change mode", ErrorCode.NOT_GLOBAL_ADMIN);
		}
		StateContext.setState("core.maintenance.start");
	}

	@Override
	public void runningMode() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can change mode", ErrorCode.NOT_GLOBAL_ADMIN);
		}
		StateContext.setState("core.maintenance.end");
	}

	@Override
	public UpgradeStatus upgradeStatus() throws ServerFault {
		ISchemaUpgradersProvider upgradersProvider = ISchemaUpgradersProvider.getSchemaUpgradersProvider();
		if (upgradersProvider == null) {
			return UpgradeStatus.create("No upgraders found. Make sure the package bm-core-upgraders is installed.",
					UpgradeStatus.State.UPGRADERS_NOT_AVAILABLE);
		}

		if (!upgradersProvider.isActive()) {

			return UpgradeStatus.create("upgraders is not active. Make sure your subscription is valid.",
					UpgradeStatus.State.UPGRADERS_NOT_RUNNABLE);
		}

		return UpgradeStatus.create("OK", UpgradeStatus.State.OK);
	}

	@Override
	public PublicInfos getInfos() {
		Map<String, String> confValues = systemConfService().getValues().values;
		PublicInfos ret = new PublicInfos();
		ret.defaultDomain = confValues.get(SysConfKeys.default_domain.name());
		ret.softwareVersion = BMVersion.getVersion();
		ret.releaseName = BMVersion.getVersionName();
		return ret;
	}

	@Override
	public void ping(String ip) throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can do upgrade", ErrorCode.NOT_GLOBAL_ADMIN);
		}

		AHCNodeClientFactory ncf = new AHCNodeClientFactory();
		INodeClient nc = ncf.create(ip);
		nc.ping();
	}

	@Override
	public List<String> getSubscriptionContacts() throws ServerFault {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);
		return systemConfService().getValues().stringList(SysConfKeys.subscription_contacts.name());
	}

	@Override
	public void setSubscriptionContacts(List<String> emails) throws ServerFault {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_SUBSCRIPTION);

		SystemConf sysConf = SystemConf.create(new HashMap<>());
		sysConf.setStringListValue(SysConfKeys.subscription_contacts.name(), emails);
		systemConfService().updateMutableValues(sysConf.values);
	}

	private ISystemConfiguration systemConfService() {
		return context.su().provider().instance(ISystemConfiguration.class);
	}

	@Override
	public String getHostReport() {
		return HostReportProvider.getHostReportService().orElseThrow().getHostReport(context);
	}

	@Override
	public String sendHostReport() {
		return HostReportProvider.getHostReportService().orElseThrow().sendHostReport(context);
	}

}
