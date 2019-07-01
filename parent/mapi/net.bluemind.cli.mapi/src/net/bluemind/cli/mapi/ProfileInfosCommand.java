/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.mapi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.vertx.java.core.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFAI;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.publicfolders.common.PublicFolders;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.todolist.api.ITodoUids;

@Command(name = "infos", description = "Show profile infos")
public class ProfileInfosCommand implements ICmdLet, Runnable {

	Set<String> mapiRelatedTypes = Sets.newHashSet(IAddressBookUids.TYPE, ICalendarUids.TYPE, MapiFolderContainer.TYPE,
			MapiFAIContainer.TYPE, IMailReplicaUids.REPLICATED_MBOXES, ITodoUids.TYPE);

	private static class SubtreeProc implements NodeProcessor {

		@Override
		public ItemValue<ContainerHierarchyNode> visit(CliContext ctx, ItemValue<ContainerHierarchyNode> node) {
			ctx.info("* SUBTREE " + node);
			IDbByContainerReplicatedMailboxes mboxesApi = ctx.adminApi()
					.instance(IDbByContainerReplicatedMailboxes.class, node.value.containerUid);
			List<ItemValue<MailboxReplica>> mailboxFolders = mboxesApi.allReplicas().stream()
					.sorted((f1, f2) -> f1.value.fullName.compareTo(f2.value.fullName)).collect(Collectors.toList());
			ctx.info("\tThe subtree has " + mailboxFolders.size() + " folder(s)");
			for (ItemValue<MailboxReplica> mf : mailboxFolders) {
				ctx.info("\t\tid: " + mf.internalId + ", uid: " + mf.uid + ", fn:" + mf.value.fullName + ",  parent: "
						+ mf.value.parentUid + ", flags: " + mf.flags);
			}
			return node;
		}

	}

	private static class MapiFolderProc implements NodeProcessor {

		@Override
		public ItemValue<ContainerHierarchyNode> visit(CliContext ctx, ItemValue<ContainerHierarchyNode> node) {
			IMapiFolder mapiFolderApi = ctx.adminApi().instance(IMapiFolder.class, node.value.containerUid);
			Count count = mapiFolderApi.count(ItemFlagFilter.all());
			ctx.info("* MAPI_FOLDER " + node.uid + "\t\t" + count.total + " item(s)");
			return node;
		}

	}

	private static class MapiFaiProc implements NodeProcessor {

		@Override
		public ItemValue<ContainerHierarchyNode> visit(CliContext ctx, ItemValue<ContainerHierarchyNode> node) {
			String parsedReplica = node.value.containerUid.substring("mapi_fai_".length());
			IMapiFolderAssociatedInformation faiApi = ctx.adminApi().instance(IMapiFolderAssociatedInformation.class,
					parsedReplica);
			List<ItemValue<MapiFAI>> allFais = faiApi.all().stream()
					.sorted((fai1, fai2) -> fai1.value.folderId.compareTo(fai2.value.folderId))
					.collect(Collectors.toList());
			ctx.info("FAIs found for replica " + parsedReplica + " => " + allFais.size() + " message(s)");
			for (ItemValue<MapiFAI> fai : allFais) {
				JsonObject content = new JsonObject(fai.value.faiJson).getObject("setProperties");
				String mClass = content.getString("PidTagMessageClass");
				ctx.info("\t* FAI " + mClass + " in folder " + fai.value.folderId);
			}
			return node;
		}

	}

	private static final NodeProcessor DEFAULT_PROC = (CliContext ctx, ItemValue<ContainerHierarchyNode> node) -> {
		ctx.info("* NODE " + node.value.containerUid);
		return node;
	};

	private static final Map<String, NodeProcessor> PROCESSORS = ImmutableMap.of(//
			IMailReplicaUids.REPLICATED_MBOXES, new SubtreeProc(), //
			MapiFolderContainer.TYPE, new MapiFolderProc(), //
			MapiFAIContainer.TYPE, new MapiFaiProc()//
	);

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mapi");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ProfileInfosCommand.class;
		}
	}

	private CliContext ctx;

	@Arguments(required = true, description = "email address or mail domain")
	public String target;

	@Override
	public void run() {
		if (!Regex.EMAIL.validate(target)) {
			CliUtils cliUtils = new CliUtils(ctx);
			String asADomain = cliUtils.getDomainUidFromDomain(target);
			if (asADomain != null) {
				target = asADomain;
				publicMailboxProfile();
			} else {
				throw new ServerFault(target + " is not an email & not a domain.");
			}
		} else {
			privateMailboxProfile();
		}

	}

	private void publicMailboxProfile() {
		String pfHier = PublicFolders.mailboxGuid(target);
		ctx.info("Process public hierarchy of domain " + target + " => " + pfHier);
		IContainersFlatHierarchy hierApi = ctx.adminApi().instance(IContainersFlatHierarchy.class, target, pfHier);
		processHierarchy(hierApi);
	}

	private void privateMailboxProfile() {
		CliUtils cliUtils = new CliUtils(ctx);
		String domainUid = cliUtils.getDomainUidFromEmailOrDomain(target);

		IMailboxes boxApi = ctx.adminApi().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mailbox = boxApi.byEmail(target);
		if (mailbox == null) {
			ctx.error("Mailbox not found for email '" + target + "'");
			return;
		}
		ctx.info("Profile " + target + " has mailbox uid " + mailbox.uid);
		IMapiMailbox mapiApi = ctx.adminApi().instance(IMapiMailbox.class, domainUid, mailbox.uid);
		MapiReplica replica = mapiApi.get();
		if (replica == null) {
			ctx.error("Missing replica for email " + target);
			return;
		}
		ctx.info("Replica local: " + replica.localReplicaGuid + ", logon: " + replica.logonReplicaGuid + ", mailbox: "
				+ replica.mailboxGuid);

		IContainersFlatHierarchy hierApi = ctx.adminApi().instance(IContainersFlatHierarchy.class, domainUid,
				mailbox.uid);
		processHierarchy(hierApi);
	}

	private void processHierarchy(IContainersFlatHierarchy hierApi) {
		List<ItemValue<ContainerHierarchyNode>> sortedAndFiltered = hierApi.list()//
				.stream()//
				.filter(v -> mapiRelatedTypes.contains(v.value.containerType))//
				.sorted((n1, n2) -> {
					int byType = n1.value.containerType.compareTo(n2.value.containerType);
					if (byType == 0) {
						return n1.uid.compareTo(n2.uid);
					} else {
						return byType;
					}
				}).map(n -> PROCESSORS.getOrDefault(n.value.containerType, DEFAULT_PROC).visit(ctx, n))
				.collect(Collectors.toList());
		ctx.info("Profile has " + sortedAndFiltered.size() + " (filtered) node(s)");
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
