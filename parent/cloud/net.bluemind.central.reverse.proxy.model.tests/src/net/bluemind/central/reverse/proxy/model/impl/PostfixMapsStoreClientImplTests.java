package net.bluemind.central.reverse.proxy.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.Sets;

import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.PostfixMapsStorage;
import net.bluemind.central.reverse.proxy.model.PostfixMapsStore;
import net.bluemind.central.reverse.proxy.model.client.PostfixMapsStoreClient;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.MemberInfo;
import net.bluemind.lib.vertx.VertxPlatform;

public class PostfixMapsStoreClientImplTests {

	private Vertx vertx;
	private PostfixMapsStorage storage;
	private PostfixMapsStore store;

	@Before
	public void setupTest() {
		vertx = VertxPlatform.getVertx();
		Set<String> domainAliases = new HashSet<>();
		domainAliases.addAll(Arrays.asList("alias1", "alias2"));
		storage = PostfixMapsStorage.create();
		store = PostfixMapsStore.create( storage);
		store.setupService(vertx);
	}

	@After
	public void tearDownTest() throws InterruptedException, ExecutionException {
		store.tearDown();
	}

	@Test
	public void noStore() throws InterruptedException, ExecutionException {
		store.tearDown();
		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext
				.asyncTest(context -> client.addDomain(new DomainInfo("domain.internal", Set.of("alias1", "alias2")))
						.onComplete(ar -> context.assertions(() -> {
							assertTrue(ar.failed());
						})));
	}

	@Test
	public void addDomain() {
		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext
				.asyncTest(context -> client.addDomain(new DomainInfo("domain.internal", Set.of("alias1", "alias2")))
						.compose(ar1 -> client.addDomain(new DomainInfo("otherdomain.internal", Collections.emptySet()))
								.andThen(ar2 -> context.assertions(() -> {
									assertTrue(ar2.succeeded());
									ar2.result();
									Collection<String> aliases = storage.domainAliases("domain.internal");
									assertEquals(2, aliases.size());
									assertTrue(aliases.contains("alias1"));
									assertTrue(aliases.contains("alias2"));
								}))));
	}

	@Test
	public void updateDomain() {
		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> client
				.addDomain(new DomainInfo("domain.internal", Set.of("alias1", "alias2")))
				.compose(ar1 -> client.addDomain(new DomainInfo("otherdomain.internal", Collections.emptySet())))
				.compose(
						ar1 -> client.addDomain(new DomainInfo("domain.internal", Set.of("alias1", "alias2", "alias3")))
								.andThen(ar2 -> context.assertions(() -> {
									assertTrue(ar2.succeeded());
									Collection<String> aliases = storage.domainAliases("domain.internal");
									assertEquals(3, aliases.size());
									assertEquals(0, Sets
											.difference(Set.of("alias1", "alias2", "alias3"), new HashSet<>(aliases))
											.size());
								}))));
	}

	@Test
	public void addSimpleMailbox_invalidEmail() {
		storage.updateDomain("domain.internal", Set.of("alias1", "alias2"));

		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> client
				.addDir(new DirInfo("domain.internal", "entry-uid", "user", false, "user-mailbox", "internal",
						Set.of(new DirEmail("useremail", true)), "datalocation"))
				.andThen(ar1 -> client.aliasToMailboxes("invalidemail").onComplete(ar2 -> context.assertions(() -> {
					assertTrue(ar2.succeeded());
					assertEquals(0, ar2.result().size());
				}))));
	}

	@Test
	public void addSimpleMailbox_validEmail() {
		storage.updateDomain("domain.internal", Set.of("alias1", "alias2"));

		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context ->
		// Create and check alias resolved
		client.addDir(new DirInfo("domain.internal", "entry-uid", "user", false, "user-mailbox", "internal",
				Set.of(new DirEmail("useremail@alias1", true)), "datalocation"))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("user-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("user-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				// Suspend mailbox and check alias is no more resolved
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "entry-uid", "user", true, "user-mailbox",
						"internal", Set.of(new DirEmail("useremail@alias1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(0, ar2.result().size());
						})))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(0, ar2.result().size());
						})))
				// Unsuspend mailbox and check alias resolved
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "entry-uid", "user", false, "user-mailbox",
						"internal", Set.of(new DirEmail("useremail@alias1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("user-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("user-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				// Delete mailbox
				.compose(ar1 -> client.removeDir("entry-uid")).compose(ar1 -> client
						.aliasToMailboxes("useremail@alias1").andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(0, ar2.result().size());
						})))
				.compose(ar1 -> client.aliasToMailboxes("useremail@alias2").andThen(ar2 -> context.assertions(() -> {
					assertTrue(ar2.succeeded());
					assertEquals(0, ar2.result().size());
				}))));
	}

	@Test
	public void addMailshare_validEmail() {
		storage.updateDomain("domain.internal", Set.of("alias1", "alias2"));

		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context ->
		// Create and check alias resolved
		client.addDir(new DirInfo("domain.internal", "entry-uid", "mailshare", false, "mailshare-mailbox", "internal",
				Set.of(new DirEmail("mailshare@alias1", true)), "datalocation"))
				.compose(ar1 -> client.aliasToMailboxes("mailshare@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("+mailshare-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("mailshare@alias2").andThen(ar2 -> context.assertions(() -> {
					assertTrue(ar2.succeeded());
					assertEquals(1, ar2.result().size());
					assertEquals("+mailshare-mailbox@domain.internal", ar2.result().iterator().next());
				}))));
	}

	@Test
	public void groupMailbox() {
		storage.updateDomain("domain.internal", Set.of("alias1", "alias2"));

		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context ->
		// Group with no mail archive
		client.addDir(new DirInfo("domain.internal", "entry-uid", "group", false, "group-mailbox", "none",
				Set.of(new DirEmail("groupemail@alias1", true)), "datalocation"))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result().isEmpty());
						})))
				// Enable group mail archive
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "entry-uid", "group", false,
						"group-mailbox", "internal", Set.of(new DirEmail("groupemail@alias1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("+group-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("+group-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				// Disable group mail archive
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "entry-uid", "group", false,
						"group-mailbox", "none", Set.of(new DirEmail("groupemail@alias1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("usergroup@alias1").andThen(ar2 -> context.assertions(() -> {
					assertTrue(ar2.succeeded());
					assertTrue(ar2.result().isEmpty());
				}))));
	}

	@Test
	public void groupMailbox_addRemoveMember() {
		storage.updateDomain("domain.internal", Set.of("alias1"));

		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context ->
		// Create a group
		client.addDir(new DirInfo("domain.internal", "group-entry-uid", "group", false, "group-mailbox", "none",
				Set.of(new DirEmail("groupemail@alias1", true)), "datalocation"))
				// Create a user
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "user1-entry-uid", "user", false,
						"user1-mailbox", "internal", Set.of(new DirEmail("user1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result().isEmpty());
						})))
				// Add user to group
				.compose(ar1 -> client.manageMember(new MemberInfo(true, "group-entry-uid", "user", "user1-entry-uid")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("user1-mailbox@domain.internal", ar2.result().iterator().next());
						})))
				// Enable group mail archive
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "group-entry-uid", "group", false,
						"group-mailbox", "internal", Set.of(new DirEmail("groupemail@alias1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(2, ar2.result().size());
							assertTrue(ar2.result().contains("user1-mailbox@domain.internal"));
							assertTrue(ar2.result().contains("+group-mailbox@domain.internal"));
						})))
				// Remove user from group
				.compose(
						ar1 -> client.manageMember(new MemberInfo(false, "group-entry-uid", "user", "user1-entry-uid")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1").andThen(ar2 -> context.assertions(() -> {
					assertTrue(ar2.succeeded());
					assertEquals(1, ar2.result().size());
					assertTrue(ar2.result().contains("+group-mailbox@domain.internal"));
				}))));
	}

	@Test
	public void groupMailbox_addMemberBeforeGroupExists() {
		storage.updateDomain("domain.internal", Set.of("alias1"));

		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context ->
		// Create a user
		client.addDir(new DirInfo("domain.internal", "user1-entry-uid", "user", false, "user1-mailbox", "internal",
				Set.of(new DirEmail("user1@alias1", true)), "datalocation"))
				// Add user to group
				.compose(ar1 -> client.manageMember(new MemberInfo(true, "group-entry-uid", "user", "user1-entry-uid")))
				// Create group with mail archive
				.compose(ar1 -> client.addDir(new DirInfo("domain.internal", "group-entry-uid", "group", false,
						"group-mailbox", "internal", Set.of(new DirEmail("groupemail@alias1", true)), "datalocation")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(2, ar2.result().size());
							assertTrue(ar2.result().contains("user1-mailbox@domain.internal"));
							assertTrue(ar2.result().contains("+group-mailbox@domain.internal"));
						})))
				// Remove user from group
				.compose(
						ar1 -> client.manageMember(new MemberInfo(false, "group-entry-uid", "user", "user1-entry-uid")))
				.compose(ar1 -> client.aliasToMailboxes("groupemail@alias1").andThen(ar2 -> context.assertions(() -> {
					assertTrue(ar2.succeeded());
					assertEquals(1, ar2.result().size());
					assertTrue(ar2.result().contains("+group-mailbox@domain.internal"));
				}))));
	}

	@Test
	public void allMailboxQueries_noSplit() {
		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> client
				.addInstallation(new InstallationInfo(null, "datalocation-uid", "datalocation-ip", false, true))
				.compose(
						ar1 -> client.addDomain(new DomainInfo("domain-uid", Set.of("domain-alias1", "domain-alias2"))))
				.compose(ar1 -> client.addDomainSettings(new DomainSettings("domain-uid", null, false)))
				.compose(ar1 -> client.addDir(new DirInfo("domain-uid", "entry-uid", "user", false, "entry-mailboxname",
						"internal", Set.of(new DirEmail("user-email@domain-alias1", true)), "datalocation-uid")))
				// Test domain managed
				.compose(ar1 -> client.mailboxDomainsManaged("domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("invalid-alias")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				// Test mailbox exists
				.compose(ar1 -> client.mailboxExists("entry-mailboxname@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-mailboxname-invalid@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-mailboxname-invalid@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-mailboxname-invalid@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				// Test email to mailbox
				.compose(ar1 -> client.aliasToMailboxes("user-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("entry-mailboxname@domain-uid", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("user-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("entry-mailboxname@domain-uid", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("user-email@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("entry-mailboxname@domain-uid", ar2.result().iterator().next());
						})))
				// Test mailbox name to mailbox store
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("lmtp:datalocation-ip:2400", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-invalid")
						.andThen(ar2 -> context.assertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						}))));
	}

	@Test
	public void allMailboxQueries_splitNotForwardUnknown() {
		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> client
				.addInstallation(new InstallationInfo(null, "datalocation-uid", "datalocation-ip", false, true))
				.compose(ar1 -> client.addDomain(new DomainInfo("domain-uid", Set.of("domain-alias1", "domain-alias2")))
						.andThen(ar2 -> context.partialAssertions(() -> assertTrue(ar2.succeeded()))))
				.compose(
						ar1 -> client
								.addDomainSettings(new DomainSettings("domain-uid", "smtp-relay", false)).andThen(
										ar2 -> context.partialAssertions(() -> assertTrue(ar2.succeeded()))))
				.compose(ar1 -> client
						.addDir(new DirInfo("domain-uid", "entry-external-uid", "user", false,
								"entry-external-mailboxname", "external",
								Set.of(new DirEmail("user-external-email@domain-alias1", true)), "datalocation-uid"))
						.andThen(ar2 -> context.partialAssertions(() -> assertTrue(ar2.succeeded()))))
				// Test domain managed
				.compose(ar1 -> client.mailboxDomainsManaged("domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("invalid-alias")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				// Test mailbox exists
				.compose(ar1 -> client.mailboxExists("entry-external-mailboxname@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("user-external-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("user-external-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-external-mailboxname-invalid@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-external-mailboxname-invalid@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				// Test email to mailbox
				.compose(ar1 -> client.aliasToMailboxes("user-external-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertEquals("user-external-email@domain-alias1", ar2.result().iterator().next());
						})))
				.compose(ar1 -> client.aliasToMailboxes("user-external-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertTrue(ar2.result().contains("user-external-email@domain-alias1")
									|| ar2.result().contains("user-external-email@domain-alias2"));
						})))
				.compose(ar1 -> client.aliasToMailboxes("user-external-email@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertTrue(ar2.result().contains("user-external-email@domain-alias1")
									|| ar2.result().contains("user-external-email@domain-alias2"));
						})))
				// Test mailbox name to mailbox store
				.compose(ar1 -> client.getMailboxRelay("user-external-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("smtp:smtp-relay:25", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("user-external-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("smtp:smtp-relay:25", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-invalid")
						.andThen(ar2 -> context.assertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						}))));
	}

	@Test
	public void allMailboxQueries_splitForwardUnknown() {
		PostfixMapsStoreClient client = PostfixMapsStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> client
				.addInstallation(new InstallationInfo(null, "datalocation-uid", "datalocation-ip", false, true))
				.compose(
						ar1 -> client.addDomain(new DomainInfo("domain-uid", Set.of("domain-alias1", "domain-alias2"))))
				.compose(ar1 -> client.addDomainSettings(new DomainSettings("domain-uid", "smtp-relay", true)))
				.compose(ar1 -> client.addDir(new DirInfo("domain-uid", "entry-external-uid", "user", false,
						"entry-external-mailboxname", "external",
						Set.of(new DirEmail("user-external-email@domain-alias1", true)), "datalocation-uid")))
				// Test domain managed
				.compose(ar1 -> client.mailboxDomainsManaged("domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxDomainsManaged("invalid-alias")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertFalse(ar2.result());
						})))
				// Test mailbox exists
				.compose(ar1 -> client.mailboxExists("entry-external-mailboxname@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("user-external-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("user-external-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-external-mailboxname-invalid@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				.compose(ar1 -> client.mailboxExists("entry-external-mailboxname-invalid@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertTrue(ar2.result());
						})))
				// Test email to mailbox
				.compose(ar1 -> client.aliasToMailboxes("user-external-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertTrue(ar2.result().contains("user-external-email@domain-alias1")
									|| ar2.result().contains("user-external-email@domain-alias2"));
						})))
				.compose(ar1 -> client.aliasToMailboxes("user-external-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertTrue(ar2.result().contains("user-external-email@domain-alias1")
									|| ar2.result().contains("user-external-email@domain-alias2"));
						})))
				.compose(ar1 -> client.aliasToMailboxes("user-external-email@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals(1, ar2.result().size());
							assertTrue(ar2.result().contains("user-external-email@domain-alias1")
									|| ar2.result().contains("user-external-email@domain-alias2"));
						})))
				// Test mailbox name to mailbox store
				.compose(ar1 -> client.getMailboxRelay("user-external-email@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("user-external-email@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("smtp:smtp-relay:25", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("user-external-email@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("smtp:smtp-relay:25", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-external-mailboxname@domain-uid")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-external-mailboxname@domain-alias1")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("smtp:smtp-relay:25", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-external-mailboxname@domain-alias2")
						.andThen(ar2 -> context.partialAssertions(() -> {
							assertTrue(ar2.succeeded());
							assertEquals("smtp:smtp-relay:25", ar2.result());
						})))
				.compose(ar1 -> client.getMailboxRelay("entry-mailboxname@domain-invalid")
						.andThen(ar2 -> context.assertions(() -> {
							assertTrue(ar2.succeeded());
							assertNull(ar2.result());
						}))));
	}
}