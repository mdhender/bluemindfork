package net.bluemind.tests.defaultdata;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.testhelper.CachesTestHelper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.persistence.ServerStore;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class PopulateHelper {

	private static final Logger logger = LoggerFactory.getLogger(PopulateHelper.class);

	static {
		System.setProperty("throttle.disabled", "true");
	}

	private PopulateHelper() {
	}

	private static void addDomainContainers(String domainUid, String... aliases) throws Exception {
		System.err.println("Populate " + domainUid + " aliases: " + Arrays.toString(aliases));
		IDomains domains = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);

		domains.create(domainUid, Domain.create(domainUid, domainUid, domainUid, Sets.newHashSet(aliases)));
	}

	public static final String FAKE_CYRUS_IP = "10.1.2.3";

	private static String installationId() throws IOException {
		return InstallationId.getIdentifier();
	}

	public static void addDomain(String domain) throws Exception {
		addDomain(domain, Routing.none);
	}

	public static void addDomain(String domain, Routing adminRouting, String... aliases) throws Exception {
		createDomain(domain, aliases);
		addDomainAdmin("admin", domain, adminRouting);
	}

	public static void createDomain(String domain, String... aliases) throws Exception {
		addDomainContainers(domain, aliases);
		IServer srvService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				installationId());
		List<ItemValue<Server>> servers = srvService.allComplete();
		for (ItemValue<Server> iv : servers) {
			for (String tag : iv.value.tags) {
				srvService.assign(iv.uid, domain, tag);
			}
		}
	}

	public static void initGlobalVirt(Server... servers) throws Exception {
		initGlobalVirt(true, servers);
	}

	public static void initGlobalVirt(boolean withCore, Server... servers) throws Exception {
		CachesTestHelper.invalidate();
		DataSource dataSource = JdbcActivator.getInstance().getDataSource();
		ContainerStore cs = new ContainerStore(dataSource, SecurityContext.SYSTEM);
		cs.create(Container.create(installationId(), "installation", "installation",
				SecurityContext.SYSTEM.getSubject(), true));

		if (cs.get(DomainsContainerIdentifier.getIdentifier()) == null) {
			cs.create(Container.create(DomainsContainerIdentifier.getIdentifier(), "domains", "domain",
					SecurityContext.SYSTEM.getSubject(), true));
		}
		addDomainContainers("global.virt");

		Server core = null;
		if (withCore) {
			core = new Server();
			core.ip = "127.0.0.1";
			core.name = "localhost";
			core.tags = Lists.newArrayList("bm/core");
			servers = (Server[]) ArrayUtils.add(servers, core);
		}

		createServers(servers);
		dispatchTopology(servers);

		boolean createFakeImap = true;
		for (Server s : servers) {
			for (String tag : s.tags) {
				if ("mail/imap".equals(tag)) {
					createFakeImap = false;
				}
			}
		}
		if (createFakeImap) {
			// Create a fake cyrus
			Server fakeImapServer = new Server();
			fakeImapServer.ip = FAKE_CYRUS_IP;
			fakeImapServer.tags = Lists.newArrayList("mail/imap");
			createServers(fakeImapServer);
		}

		logger.info("GLOBAL.VIRT INITIALIZED");
	}

	private static void dispatchTopology(Server... servers) {
		IServer yeahApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");
		List<ItemValue<Server>> allSrvs = yeahApi.allComplete();

		Topology.update(allSrvs);

	}

	public static ItemValue<Domain> createTestDomain(String domainUid, Server... servers) throws Exception {

		return createTestDomain(domainUid,
				Domain.create(domainUid, domainUid, "", new HashSet<String>(Arrays.asList("alias" + domainUid))),
				servers);
	}

	public static ItemValue<Domain> createTestDomain(String domainUid, Domain domain, Server... servers)
			throws Exception {

		IDomains domains = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		domains.create(domainUid, domain);

		ContainerStore cs = new ContainerStore(JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);

		ServerStore serverStore = new ServerStore(JdbcActivator.getInstance().getDataSource(),
				cs.get(installationId()));

		boolean createFakeImap = true;

		for (Server s : servers) {
			for (String tag : s.tags) {
				if ("mail/imap".equals(tag)) {
					createFakeImap = false;
				}
				logger.info("Assign {} to {} for {}", tag, s.address(), domainUid);
				serverStore.assign(s.ip, domainUid, tag);
			}
		}

		if (createFakeImap) {
			serverStore.assign(FAKE_CYRUS_IP, domainUid, "mail/imap");
		}

		serverStore.assign("127.0.0.1", domainUid, "bm/core");
		return domains.get(domainUid);
	}

	public static void unAssignFakeCyrus(String domainUid) throws SQLException, IOException {
		ContainerStore cs = new ContainerStore(JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);
		ServerStore serverStore = new ServerStore(JdbcActivator.getInstance().getDataSource(),
				cs.get(installationId()));

		serverStore.unassign(FAKE_CYRUS_IP, domainUid, "mail/imap");
	}

	public static void domainAdmin(String domainUid, String userUid) throws Exception {

		aclAdmin(installationId(), userUid);

		aclAdmin(domainUid, userUid);
		aclAdmin("users_" + domainUid, userUid);
		aclAdmin("groups_" + domainUid, userUid);
		aclAdmin("addressbook_" + domainUid, userUid);
		aclAdmin("mboxes_" + domainUid, userUid);

	}

	private static void aclAdmin(String containerUid, String subject) throws Exception {
		AclStore aclStore = new AclStore(JdbcActivator.getInstance().getDataSource());
		ContainerStore cs = new ContainerStore(JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);
		Container c = cs.get(containerUid);
		if (c != null) {
			logger.info("Adding Verb.All for {} to {}", subject, containerUid);
			aclStore.store(c, Arrays.asList(AccessControlEntry.create(subject, Verb.All)));
		}

	}

	/**
	 * <i>Note: we use {@link Server#ip} for {@link ItemValue#uid} and
	 * {@link ItemValue#displayName}.</i>
	 */
	public static void createServers(Server... servers) throws ServerFault {
		DataSource dataSource = JdbcActivator.getInstance().getDataSource();
		CachesTestHelper.invalidate();

		ContainerStore cs = new ContainerStore(dataSource, SecurityContext.SYSTEM);
		try {
			Container container = cs.get(InstallationId.getIdentifier());

			ServerStore serverStore = new ServerStore(dataSource, container);
			ContainerStoreService<Server> storeService = new ContainerStoreService<>(dataSource, SecurityContext.SYSTEM,
					container, serverStore);

			for (Server server : servers) {
				Assert.assertNotNull("server ip cannot be null " + server, server.ip);
				if (StringUtils.isBlank(server.name)) {
					server.name = server.ip;
				}
				if (StringUtils.isBlank(server.fqdn)) {
					server.fqdn = server.name;
				}
				storeService.create(server.ip, server.ip, server);
				logger.info("******** Created " + server.ip);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	public static void addGlobalVirt(DataSource pool) throws Exception {
		ContainerStore cs = new ContainerStore(pool, SecurityContext.SYSTEM);
		cs.create(Container.create(installationId(), "installation", "installation",
				SecurityContext.SYSTEM.getSubject(), true));

		addDomainContainers("global.virt");

		IServer srvService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				installationId());

		Server host = new Server();
		host.ip = new BmConfIni().get("host");
		host.tags = Lists.newArrayList("mail/imap", "bm/pgsql", "bm/es");
		srvService.create("vm", host);

		Server core = new Server();
		core.ip = "127.0.0.1";
		core.tags = Lists.newArrayList("bm/core");
		srvService.create("localhost", core);

		addDomainAdmin("admin0", "global.virt");

	}

	public static String addDomainAdmin(String login, String domain) throws ServerFault, IOException {
		return addDomainAdmin(login, domain, Mailbox.Routing.none);
	}

	public static String addDomainAdmin(String login, String domain, Mailbox.Routing routing)
			throws ServerFault, IOException {
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
		User admin = new User();
		admin.login = login;
		admin.password = "admin";// NOSONAR
		admin.routing = routing;

		admin.dataLocation = new BmConfIni().get("imap-role") != null ? new BmConfIni().get("imap-role")
				: FAKE_CYRUS_IP;

		Email em = new Email();
		em.address = login + "@" + domain;
		em.isDefault = true;
		em.allAliases = false;

		Email alias = new Email();
		alias.address = login + "-alias@" + domain;
		alias.isDefault = false;
		alias.allAliases = false;

		Email allAliases = new Email();
		allAliases.address = login + "-allalias@" + domain;
		allAliases.isDefault = false;
		allAliases.allAliases = true;

		admin.emails = Arrays.asList(em, alias, allAliases);

		VCard card = new VCard();
		card.identification.name = Name.create(domain.toUpperCase(), login, null, null, null, null);
		card.identification.formatedName = FormatedName.create(login);
		admin.contactInfos = card;

		if (domain.equals("global.virt")) {
			admin.system = true;
		}

		String uid = login;
		userService.create(uid, admin);
		if (domain.equals("global.virt")) {
			userService.setRoles(uid, new HashSet<>(Arrays.asList(SecurityContext.ROLE_SYSTEM)));
		} else {
			userService.setRoles(uid, new HashSet<>(Arrays.asList(SecurityContext.ROLE_ADMIN)));
		}
		logger.info("******* login '{}' @ domain '{}' CREATED with uid {} *******", login, domain, uid);
		return uid;
	}

	public static String addUser(String login, String domain) throws ServerFault, IOException {
		return addUser(login, domain, Mailbox.Routing.none);
	}

	public static String addUserWithRoles(String login, String domain, String... roles)
			throws ServerFault, IOException {
		return addUser(login, domain, Mailbox.Routing.none, roles);
	}

	public static String addUser(String login, String domain, Mailbox.Routing mailrouting, String... roles)
			throws ServerFault {
		User admin = getUser(login, domain, mailrouting);
		return addUser(domain, admin, roles);
	}

	public static String addSimpleUser(String login, String domain, Mailbox.Routing mailrouting, String... roles)
			throws ServerFault {
		User u = getUser(login, domain, mailrouting);
		u.accountType = AccountType.SIMPLE;
		return addUser(domain, u, roles);
	}

	public static String addUser(String domain, User user, String... roles) {
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
		String uid = user.login;
		userService.create(uid, user);
		Set<String> setRoles = new HashSet<String>(Arrays.asList(roles));
		if (!setRoles.isEmpty()) {
			userService.setRoles(uid, setRoles);
		}
		return uid;
	}

	public static User getUser(String login, String domain, Mailbox.Routing mailrouting) {
		User admin = new User();
		admin.login = login;
		admin.password = login;
		admin.routing = mailrouting;

		admin.dataLocation = new BmConfIni().get("imap-role") != null ? new BmConfIni().get("imap-role")
				: FAKE_CYRUS_IP;

		admin.emails = Arrays.asList(Email.create(login + "@" + domain, true, false));
		VCard card = new VCard();
		card.identification.name = Name.create(domain.toUpperCase(), login, null, null, null, null);
		card.identification.formatedName = FormatedName.create(login);
		admin.contactInfos = card;

		return admin;
	}

	public static String addExternalUser(String domain, String email, String name) {
		IExternalUser externalUserService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IExternalUser.class, domain);
		String uid = "extUserUID_" + System.nanoTime();
		ExternalUser externalUser = new ExternalUser();
		externalUser.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		externalUser.contactInfos = new VCard();
		externalUser.contactInfos.identification.name = VCard.Identification.Name.create(name, null, null, null, null,
				null);
		externalUser.contactInfos.communications.emails = new ArrayList<>();
		externalUser.contactInfos.communications.emails.add(VCard.Communications.Email.create(email));
		externalUser.emails = new ArrayList<>();
		externalUser.emails.add(Email.create(email, true));
		externalUserService.create(uid, externalUser);
		return uid;
	}

	public static void assign(DataSource pool, String serverUid, String tag, String domainUid)
			throws ServerFault, IOException {

		ContainerStore cs = new ContainerStore(pool, SecurityContext.SYSTEM);
		try {
			Container container = cs.get(InstallationId.getIdentifier());

			ServerStore serverStore = new ServerStore(pool, container);

			serverStore.assign(serverUid, domainUid, tag);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	public static void createDomainSettings(String domainUid, Map<String, String> domainSettings) throws ServerFault {
		IDomainSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		settingsService.set(domainSettings);
	}

	public static void addOrgUnit(String domainUid, String uid, String name, String parentUid) {
		IOrgUnits orgUnits = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IOrgUnits.class,
				domainUid);
		OrgUnit ou = new OrgUnit();
		ou.name = name;
		ou.parentUid = parentUid;
		orgUnits.create(uid, ou);
	}

	public static String addGroup(String domainUid, String uid, String name, List<Member> members) {
		IGroup groups = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class, domainUid);
		Group grp = new Group();
		grp.name = name;
		groups.create(uid, grp);
		groups.add(uid, members);
		return uid;
	}

}
