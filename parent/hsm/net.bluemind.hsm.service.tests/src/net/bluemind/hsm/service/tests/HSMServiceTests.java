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
package net.bluemind.hsm.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.hsm.api.Demote;
import net.bluemind.hsm.api.IHSM;
import net.bluemind.hsm.api.Promote;
import net.bluemind.hsm.api.TierChangeResult;
import net.bluemind.hsm.processor.HSMContext;
import net.bluemind.hsm.processor.HSMContext.HSMLoginContext;
import net.bluemind.hsm.processor.HSMRunStats;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.Summary;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.utils.FileUtils;

public class HSMServiceTests {
	static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	private BmContext ctx;
	private Server imapServer;

	protected String domainUid;
	private String adminUid;
	private String adminLogin;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "bm.lan";

		Server archive = new Server();
		archive.ip = new BmConfIni().get("node-host");
		archive.tags = Lists.newArrayList("mail/archive");

		imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server esServer = new Server();
		esServer.ip = new BmConfIni().get("es-host");
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer, imapServer, archive);

		PopulateHelper.createTestDomain(domainUid, esServer, imapServer, archive);

		// create domain parititon on cyrus
		new CyrusService(imapServer.ip).createPartition(domainUid);
		new CyrusService(imapServer.ip).refreshPartitions(Arrays.asList(domainUid));

		new CyrusService(imapServer.ip).reload();

		adminLogin = "admin" + System.currentTimeMillis();
		adminUid = PopulateHelper.addDomainAdmin(adminLogin, domainUid, Routing.internal);
		PopulateHelper.domainAdmin(domainUid, adminUid);
		ctx = BmTestContext.contextWithSession(adminUid, adminUid, domainUid, BasicRoles.ROLE_ADMIN);

		ISystemConfiguration systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);
	}

	protected IHSM getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IHSM.class, domainUid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testPromoteMultiple() throws Exception {
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, adminLogin + "@" + domainUid, "admin");
				InputStream mailContent = getClass().getClassLoader().getResourceAsStream("data/test.eml");
				InputStream mailContent2 = getClass().getClassLoader().getResourceAsStream("data/test.eml")) {

			sc.login();

			int id = sc.append("INBOX", mailContent, new FlagsList());
			int id2 = sc.append("INBOX", mailContent2, new FlagsList());

			HSMContext context = getHSMContext(ctx, adminUid);
			List<TierChangeResult> demoted = demote(context, "INBOX",
					Arrays.asList(Demote.create(adminUid, "INBOX", id), Demote.create(adminUid, "INBOX", id2)));

			assertEquals(2, demoted.size());

			sc.select("INBOX");

			SearchQuery sq = new SearchQuery();
			sq.setKeyword(Flag.BMARCHIVED.toString());
			Collection<Integer> archived = sc.uidSearch(sq);
			assertEquals(2, archived.size());

			IDSet idset = IDSet.create(archived.iterator());

			List<Promote> toPromote = new ArrayList<Promote>(archived.size());

			idset.forEach(idRange -> {
				String smallerRange = idRange.toString();
				Collection<Summary> imapSummaries = sc.uidFetchSummary(smallerRange);
				for (Summary sum : imapSummaries) {
					Promote promote = new Promote();
					promote.folder = "INBOX";
					promote.imapUid = sum.getUid();
					promote.mailboxUid = adminUid;
					promote.hsmId = sum.getHeaders().getRawHeader("X-BM_HSM_ID");
					promote.internalDate = sum.getDate();
					promote.flags = sum.getFlags().asTags();
					toPromote.add(promote);
				}
			});

			List<TierChangeResult> promote = getService(ctx.getSecurityContext()).promoteMultiple(toPromote);
			assertEquals(2, promote.size());

			sc.select("INBOX");

			String expectedContent = FileUtils
					.streamString(getClass().getClassLoader().getResourceAsStream("data/test.eml"), true);
			promote.forEach(p -> {
				try (IMAPByteSource msg = sc.uidFetchMessage(p.imapId)) {
					String content = new String(msg.source().read());
					assertEquals(expectedContent, content);
				} catch (IOException e) {
					fail(e.getMessage());
				}
			});

			Collection<FlagsList> flags = sc
					.uidFetchFlags(promote.stream().map(p -> p.imapId).collect(Collectors.toList()));
			assertEquals(2, flags.size());

			flags.forEach(f -> {
				assertFalse(f.contains("bmarchived"));
			});

		}
	}

	private HSMContext getHSMContext(BmContext bmContext, String userUid) throws ServerFault {

		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, bmContext.getSecurityContext().getContainerUid()).getComplete(userUid);
		ItemValue<Server> server = bmContext.su().provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(user.value.dataLocation);

		HSMLoginContext loginContext = new HSMLoginContext(user.value.login, user.uid, server.value.address());
		return HSMContext.get(bmContext.getSecurityContext(), loginContext);
	}

	private List<TierChangeResult> demote(HSMContext context, String folderPath, Collection<Demote> demote) {

		try (StoreClient sc = context.connect(folderPath)) {

			InternalDate[] ids = sc
					.uidFetchInternalDate(demote.stream().map(d -> d.imapId).collect(Collectors.toList()));
			if (ids.length == 0) {
				throw new ServerFault("The mail id " + demote + " to demote was not found in folder " + folderPath);
			}

			List<InternalDate> dates = new ArrayList<InternalDate>(ids.length);
			Collections.addAll(dates, ids);
			DemoteCommand dc = new DemoteCommand(folderPath, sc, context, dates, Optional.empty());
			HSMRunStats stats = new HSMRunStats();
			return dc.run(stats);

		} catch (IMAPException | IOException e) {
			throw new ServerFault(e);
		}
	}
}
