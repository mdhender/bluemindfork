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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.user.api.IUserSubscription;

public class CoreEmailHelper {

	public static long getUserMailFolderAsRoot(String user, String domain, String folderName) {
		return getUserMailFolderId(user, domain, folderName, SecurityContext.SYSTEM);
	}

	public static long getUserMailFolderId(String user, String domain, String folderName, SecurityContext context) {
		List<ItemValue<ContainerHierarchyNode>> list = ServerSideServiceProvider.getProvider(context)
				.instance(IContainersFlatHierarchy.class, domain, user).list();
		return list.stream().filter(node -> {
			return node.value.name.equals(folderName);
		}).findFirst().map(node -> node.internalId).orElse(-1l);
	}

	public static ItemValue<ContainerHierarchyNode> getUserMailFolder(String user, String domain, String folderName,
			SecurityContext context) {
		List<ItemValue<ContainerHierarchyNode>> list = ServerSideServiceProvider.getProvider(context)
				.instance(IContainersFlatHierarchy.class, domain, user).list();
		return list.stream().filter(node -> {
			return node.value.name.equals(folderName);
		}).findFirst().orElse(null);
	}

	public static List<String> getUserMailFolderNames(String user, String domain, SecurityContext context) {
		List<ItemValue<ContainerHierarchyNode>> list = ServerSideServiceProvider.getProvider(context)
				.instance(IContainersFlatHierarchy.class, domain, user).list();
		return list.stream().map(node -> node.value.name).toList();
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

	public static ItemValue<MailboxItem> getMailAsRoot(String domainUid, String serverId, String userUid) {
		return getMail(domainUid, serverId, userUid, SecurityContext.SYSTEM);
	}

	private static long extractFolderId(String serverId) {
		String value = serverId;
		if (serverId.contains(SyncHelper.SHARED_SEPARATOR)) {
			value = serverId.split(SyncHelper.SHARED_SEPARATOR)[1];
		}
		return Long.parseLong(value.split(":")[0]);
	}

	private static String getFolderUid(String domainUid, String serverId, String userUid) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		long folderId = extractFolderId(serverId);
		List<ItemValue<ContainerHierarchyNode>> list = provider
				.instance(IContainersFlatHierarchy.class, domainUid, userUid).list();
		return list.stream().filter(node -> node.internalId == folderId).findFirst()
				.map(node -> node.value.containerUid).orElse("-1");
	}

	public static ItemValue<MailboxItem> getMail(String domainUid, String serverId, String userUid,
			SecurityContext context) {
		String folderUid = getFolderUid(domainUid, serverId, userUid);
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(context);
		long itemId = Long.parseLong(serverId.split(":")[1]);
		IMailboxItems dbRecApi = provider.instance(IMailboxItems.class, IMailReplicaUids.getUniqueId(folderUid));

		return dbRecApi.getCompleteById(itemId);
	}

//	public static List<ItemValue<ContainerSubscriptionModel>> shareFolder(String domainUid, String containerUid,
//			String user2, Verb verb, SecurityContext context) {
//		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
//		IContainerManagement aclApi = provider.instance(IContainerManagement.class, containerUid);
//		List<AccessControlEntry> accessControlList = aclApi.getAccessControlList();
//		accessControlList.add(AccessControlEntry.create(user2, verb));
//		aclApi.setAccessControlList(accessControlList);
//
//		IUserSubscription subService = provider.instance(IUserSubscription.class, domainUid);
//		subService.subscribe(context.getSubject(), Arrays.asList(ContainerSubscription.create(containerUid, true)));
//
//		return provider.instance(IOwnerSubscriptions.class, domainUid, user2).list().stream()
//				.filter(s -> s.displayName.equals(containerUid)).collect(Collectors.toList());
//
//	}

	public static List<ItemValue<ContainerSubscriptionModel>> shareMailbox(String domainUid, String containerUid,
			String user2, Verb verb, SecurityContext context) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IContainerManagement aclApi = provider.instance(IContainerManagement.class, containerUid);
		List<AccessControlEntry> accessControlList = aclApi.getAccessControlList();
		accessControlList.add(AccessControlEntry.create(user2, verb));
		aclApi.setAccessControlList(accessControlList);

		IUserSubscription subService = provider.instance(IUserSubscription.class, domainUid);
		subService.subscribe(context.getSubject(), Arrays.asList(ContainerSubscription.create(containerUid, true)));

		return provider.instance(IOwnerSubscriptions.class, domainUid, user2).list().stream()
				.filter(s -> s.displayName.equals(containerUid)).collect(Collectors.toList());

	}
}
