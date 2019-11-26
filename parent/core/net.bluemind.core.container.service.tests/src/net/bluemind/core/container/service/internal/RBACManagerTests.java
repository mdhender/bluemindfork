package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class RBACManagerTests {

	private String domainUid;
	private AclStore aclStore;
	private ContainerStore containerStore;
	private String userUid;
	private BmTestContext testContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		domainUid = "bmtest.lan";
		PopulateHelper.createTestDomain(domainUid);

		userUid = PopulateHelper.addUser("toto", domainUid, Routing.none);
		containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		testContext = new BmTestContext(SecurityContext.SYSTEM);
		aclStore = new AclStore(testContext, JdbcTestHelper.getInstance().getDataSource());
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@Test
	public void containerImplicitPermissions() throws SQLException {
		Container create = Container.create("tContainer", "test", "test", "testUser", domainUid);
		Container container1 = containerStore.create(create);

		// implicit owner read right
		assertTrue(
				RBACManager.forContext(context("testUser", domainUid)).forContainer(container1).can(Verb.Read.name()));
		assertFalse(RBACManager.forContext(context("notSameUser", domainUid)).forContainer(container1)
				.can(Verb.Read.name()));
		// not same domain, no implicit right on this container
		assertFalse(RBACManager.forContext(context("testUser", "fakeDomain")).forContainer(container1)
				.can(Verb.Read.name()));

		assertTrue(
				RBACManager.forContext(context("testUser", domainUid)).forContainer(container1).can(Verb.Write.name()));
		assertTrue(RBACManager.forContext(context("testUser", domainUid)).forContainer(container1)
				.can(Verb.Manage.name()));
		assertTrue(
				RBACManager.forContext(context("testUser", domainUid)).forContainer(container1).can(Verb.All.name()));
	}

	@Test
	public void containerAnonymousAclShouldNotVerifyDomain() throws SQLException {
		Container create = Container.create("tContainer", "test", "test", "testUser", domainUid);
		Container container1 = containerStore.create(create);

		assertFalse(RBACManager.forContext(new BmTestContext(SecurityContext.ANONYMOUS)).forContainer(container1)
				.can(Verb.Read.name()));

		// permission from acl
		aclStore.store(container1, Arrays.asList( //
				AccessControlEntry.create("anonymous", Verb.Read) //
		));

		// read
		assertTrue(RBACManager.forContext(new BmTestContext(SecurityContext.ANONYMOUS)).forContainer(container1)
				.can(Verb.Read.name()));
	}

	@Test
	public void containerPermAcl() throws SQLException {
		Container create = Container.create("tContainer", "test", "test", "testUser", domainUid);
		Container container1 = containerStore.create(create);

		// permission from acl
		aclStore.store(container1, //
				Arrays.asList( //
						AccessControlEntry.create("notSameUser", Verb.Read), //
						AccessControlEntry.create("aGroup", Verb.Read), //
						AccessControlEntry.create("writeGiveRead", Verb.Write), //
						AccessControlEntry.create("manageGiveNothingMore", Verb.Manage), //
						AccessControlEntry.create("allGiveAll", Verb.All) //
				));

		// read
		assertTrue(RBACManager.forContext(context("notSameUser", domainUid)).forContainer(container1)
				.can(Verb.Read.name()));
		// only apply on same domain
		assertFalse(RBACManager.forContext(context("notSameUser", "fakeDomain")).forContainer(container1)
				.can(Verb.Read.name()));

		// permission from acl from group
		assertFalse(RBACManager.forContext(context("notSameUser2", domainUid).withGroup("aFakeGroup"))
				.forContainer(container1).can(Verb.Read.name()));
		assertTrue(RBACManager.forContext(context("notSameUser2", domainUid).withGroup("aGroup"))
				.forContainer(container1).can(Verb.Read.name()));

		// permission Write
		assertTrue(RBACManager.forContext(context("writeGiveRead", domainUid)).forContainer(container1)
				.can(Verb.Write.name()));
		assertTrue(RBACManager.forContext(context("writeGiveRead", domainUid)).forContainer(container1)
				.can(Verb.Read.name()));

		// manage
		assertTrue(RBACManager.forContext(context("manageGiveNothingMore", domainUid)).forContainer(container1)
				.can(Verb.Manage.name()));
		assertFalse(RBACManager.forContext(context("manageGiveNothingMore", domainUid)).forContainer(container1)
				.can(Verb.Read.name()));
		assertFalse(RBACManager.forContext(context("manageGiveNothingMore", domainUid)).forContainer(container1)
				.can(Verb.All.name()));
		assertFalse(RBACManager.forContext(context("manageGiveNothingMore", domainUid)).forContainer(container1)
				.can(Verb.Write.name()));

		// all
		assertTrue(
				RBACManager.forContext(context("allGiveAll", domainUid)).forContainer(container1).can(Verb.All.name()));
		assertTrue(RBACManager.forContext(context("allGiveAll", domainUid)).forContainer(container1)
				.can(Verb.Write.name()));
		assertTrue(RBACManager.forContext(context("allGiveAll", domainUid)).forContainer(container1)
				.can(Verb.Read.name()));
		assertTrue(RBACManager.forContext(context("allGiveAll", domainUid)).forContainer(container1)
				.can(Verb.Manage.name()));

		// all users of domain
		// permission from acl
		aclStore.store(container1, Arrays.asList( //
				AccessControlEntry.create(domainUid, Verb.Manage)));

		assertTrue(RBACManager.forContext(context("fakeUser", domainUid)).forContainer(container1)
				.can(Verb.Manage.name()));
		assertFalse(RBACManager.forContext(context("fakeUser", "fakeDomain")).forContainer(container1)
				.can(Verb.Manage.name()));
	}

	@Test
	public void domainPermissions() {
		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRoles(ImmutableSet.of(BasicRoles.ROLE_ADMIN)))
				.forDomain(domainUid).can(BasicRoles.ROLE_ADMIN));

		assertFalse(RBACManager
				.forContext(context("domainAdmin", "fake.fr").withRoles(ImmutableSet.of(BasicRoles.ROLE_ADMIN)))
				.forDomain(domainUid).can(BasicRoles.ROLE_ADMIN));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", "fake.fr").withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_DOMAIN)))
				.forDomain(domainUid).can(BasicRoles.ROLE_ADMIN));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", "fake.fr").withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_DOMAIN)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", "fake.fr").withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_DOMAIN)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP));

		assertFalse(RBACManager
				.forContext(context("domainAdmin", "fake.fr").withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid)
						.withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS));

		assertFalse(RBACManager
				.forContext(context("domainAdmin", domainUid)
						.withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS)))
				.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_GROUP));

		assertTrue(RBACManager.forContext(context("domainAdmin", domainUid) //
				.withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP))).forDomain(domainUid)
				.can(BasicRoles.ROLE_MANAGER));

		assertTrue(RBACManager.forContext(context("domainAdmin", domainUid) //
				.withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS))).forDomain(domainUid)
				.can(BasicRoles.ROLE_MANAGER));
	}

	@Test
	public void domainPermissionsCanSelf() {
		
		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRoles(ImmutableSet.of(BasicRoles.ROLE_ADMIN)))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_USER)))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid)
						.withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES)))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES));

		// Self permission must not give any permission on domain scope for USER kind
		Set<Permission> perms = RBACManager
				.forContext(
						context("domainAdmin", domainUid).withRoles(ImmutableSet.of(BasicRoles.ROLE_MANAGE_MAILSHARE)))
				.forDomain(domainUid).resolve();
		Optional<DirEntryPermission> match = perms.stream().filter(perm -> perm instanceof DirEntryPermission)
				.map((perm) -> ((DirEntryPermission) perm)).filter(perm -> !perm.getKind().equals(Kind.MAILSHARE))
				.findFirst();
		assertFalse(match.isPresent());
	}

	@Test
	public void orgUnitPermissions() {
		// init ou
		IOrgUnits orgUnits = testContext.provider().instance(IOrgUnits.class, domainUid);
		OrgUnit rootOrgUnit = new OrgUnit();
		rootOrgUnit.name = "root";
		orgUnits.create("rootOrgUnit", rootOrgUnit);

		OrgUnit childOrgUnit = new OrgUnit();
		childOrgUnit.name = "child";
		childOrgUnit.parentUid = "rootOrgUnit";
		orgUnits.create("childOrgUnit", childOrgUnit);

		ItemValue<User> user = testContext.provider().instance(IUser.class, domainUid).getComplete(userUid);
		user.value.orgUnitUid = "childOrgUnit";
		testContext.provider().instance(IUser.class, domainUid).update(user.uid, user.value);

		assertTrue(RBACManager.forContext( //
				context("domainAdmin", domainUid).withRolesOnOrgUnit("childOrgUnit", BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_MANAGE_USER));

		assertTrue(RBACManager.forContext( //
				context("domainAdmin", domainUid)//
						.withRolesOnOrgUnit("childOrgUnit", BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_MANAGE_USER_PASSWORD));

		assertTrue(RBACManager.forContext( //
				context("domainAdmin", domainUid)//
						.withRolesOnOrgUnit("childOrgUnit", BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_MANAGE_MAILBOX));

		assertTrue(RBACManager.forContext( //
				context("domainAdmin", domainUid)//
						.withRolesOnOrgUnit("childOrgUnit", BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRolesOnOrgUnit("rootOrgUnit",
						BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_MANAGE_USER));

		assertTrue(RBACManager
				.forContext(context("domainAdmin", domainUid).withRolesOnOrgUnit("rootOrgUnit",
						BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forOrgUnit("childOrgUnit").can(BasicRoles.ROLE_MANAGE_USER));

		assertFalse(RBACManager
				.forContext(context("domainAdmin", domainUid).withRolesOnOrgUnit("childOrgUnit",
						BasicRoles.ROLE_MANAGE_USER))
				.forDomain(domainUid).forOrgUnit("rootOrgUnit").can(BasicRoles.ROLE_MANAGE_USER));

	}

	@Test
	public void selfPermissions() {

		// ROLE_SELF_CHANGE_MAIL_IDENTITIES gives
		// ROLE_MANAGE_USER_MAIL_IDENTITIES on self edit
		assertTrue(RBACManager.forContext( //
				context(userUid, domainUid).withRoles(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)).forDomain(domainUid)
				.forEntry(userUid).can(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES));

		assertFalse(RBACManager.forContext( //
				context("fakeUserNotSameAsUserUid", domainUid).withRoles(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES))
				.forDomain(domainUid).forEntry(userUid).can(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES));

		assertFalse(RBACManager.forContext( //
				context(userUid, domainUid)).forDomain(domainUid).forEntry(userUid)
				.can(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES));

	}

	private BmTestContext context(String subject, String domainUid) {
		return new BmTestContext(
				new SecurityContext(null, subject, Collections.emptyList(), Collections.emptyList(), domainUid));
	}
}
