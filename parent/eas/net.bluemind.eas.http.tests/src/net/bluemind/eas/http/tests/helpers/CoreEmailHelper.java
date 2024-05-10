/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.helpers;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;

public class CoreEmailHelper {

	public static long getUserMailFolder(String user, String domain, String folderName) {
		List<ItemValue<ContainerHierarchyNode>> list = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainersFlatHierarchy.class, domain, user).list();
		return list.stream().filter(node -> {
			return node.value.name.equals(folderName);
		}).findFirst().map(node -> node.internalId).orElse(-1l);
	}

	public static void addMail(String login, String password, String folder, String eml) {
		imapAction(login, password, sc -> {
			int added = sc.append(folder, lookupEml(eml), new FlagsList());
			assertTrue(added > 0);
		});
	}

	private static InputStream lookupEml(String eml) {
		return CoreEmailHelper.class.getResourceAsStream("/data/" + eml);
	}

	private static void imapAction(String imapLogin, String imapPass, Consumer<StoreClient> actions) {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, imapLogin, imapPass)) {
			assertTrue(sc.login());
			actions.accept(sc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static ItemValue<MailboxItem> getMail(String domainUid, String serverId) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		long folderId = Long.parseLong(serverId.split(":")[0]);
		List<ItemValue<ContainerHierarchyNode>> list = provider
				.instance(IContainersFlatHierarchy.class, domainUid, "user").list();
		String uid = list.stream().filter(node -> node.internalId == folderId).findFirst()
				.map(node -> node.value.containerUid).orElse("-1");

		long itemId = Long.parseLong(serverId.split(":")[1]);
		IMailboxItems dbRecApi = provider.instance(IMailboxItems.class, IMailReplicaUids.getUniqueId(uid));

		return dbRecApi.getCompleteById(itemId);
	}
}
