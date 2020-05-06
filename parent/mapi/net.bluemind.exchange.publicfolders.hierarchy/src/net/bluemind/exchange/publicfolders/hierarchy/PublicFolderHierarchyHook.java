/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.exchange.publicfolders.hierarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.exchange.publicfolders.common.PublicFolders;
import net.bluemind.group.api.IGroup;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class PublicFolderHierarchyHook implements IContainersHook, IAclHook {

	private static final Logger logger = LoggerFactory.getLogger(PublicFolderHierarchyHook.class);
	private final Registry reg;
	private final IdFactory idf;

	public PublicFolderHierarchyHook() {
		reg = MetricsRegistry.get();
		idf = new IdFactory("repair-needed", reg, PublicFolderHierarchyHook.class);
	}

	private Counter failuresCounter(String domainUid) {
		return reg.counter(idf.name("pf.hierarchy").withTag("domain", domainUid));
	}

	private IInternalContainersFlatHierarchy hierarchy(BmContext ctx, ContainerDescriptor cd) {
		if (cd.domainUid == null || cd.owner == null) {
			return null;
		}
		try {
			return ctx.provider().instance(IInternalContainersFlatHierarchy.class, cd.domainUid,
					PublicFolders.mailboxGuid(cd.domainUid));
		} catch (ServerFault sf) {
			logger.warn("Missing PF hierarchy container {}", cd);
			failuresCounter(cd.domainUid).increment();
			return null;
		}
	}

	private DirEntry getOwner(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		if (cd.owner == null || cd.domainUid == null) {
			return null;
		}
		if (cd.owner.equals(PublicFolders.mailboxGuid(cd.domainUid))) {
			return PublicFolders.dirEntry(cd.domainUid);
		} else {
			IDirectory dir = ctx.provider().instance(IDirectory.class, cd.domainUid);
			return dir.findByEntryUid(cd.owner);
		}
	}

	private String uid(ContainerDescriptor cd) {
		return ContainerHierarchyNode.uidFor(cd.uid, cd.type, cd.domainUid);
	}

	@FunctionalInterface
	private static interface HierarchyOperation {
		void accept(IInternalContainersFlatHierarchy hierarchy) throws ServerFault;
	}

	private void hierarchyOp(BmContext ctx, ContainerDescriptor cd, HierarchyOperation operation) {
		DirEntry owner = getOwner(ctx, cd);
		if (owner == null) {
			logger.warn("Owner not found in directory, Nothing to do on {} owned by {}", cd.uid, cd.owner);
		} else if (owner.kind == Kind.MAILSHARE || owner.kind == Kind.CALENDAR || owner.kind == Kind.ADDRESSBOOK) {
			if (cd.uid.equals(IAddressBookUids.userVCards(cd.domainUid))) {
				logger.warn("Nothing to do on directory ab {}", cd.uid);
			} else {
				logger.info("Operation with owner.kind {}", owner.kind);
				IInternalContainersFlatHierarchy service = hierarchy(ctx, cd);
				if (service != null) {
					try {
						operation.accept(service);
					} catch (ServerFault sf) {
						failuresCounter(cd.domainUid).increment();
						throw sf;
					}
				}
			}
		}
	}

	@Override
	public void onContainerCreated(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		hierarchyOp(ctx, cd, hier -> {
			logger.info("Container created {}, should create in hierarchy", cd);
			String hierUid = uid(cd);
			hier.create(hierUid, ContainerHierarchyNode.of(cd));
		});
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
		hierarchyOp(ctx, cur, hier -> {
			logger.info("Container updated from {} to {}, should update in hierarchy", prev, cur);
			hier.update(uid(cur), ContainerHierarchyNode.of(cur));
		});

	}

	@Override
	public void onContainerSettingsChanged(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		hierarchyOp(ctx, cd, hier -> {
			logger.info("Container settings updated for {}, should update in hierarchy", cd);
			hier.update(uid(cd), ContainerHierarchyNode.of(cd));
		});

	}

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		hierarchyOp(ctx, cd, hier -> {
			logger.info("Container deleted {}, should delete in hierarchy", cd);
			hier.delete(uid(cd));
		});

	}

	@Override
	public void onContainerSubscriptionsChanged(BmContext ctx, ContainerDescriptor cd, List<String> subs,
			List<String> unsubs) throws ServerFault {
	}

	@Override
	public void onContainerOfflineSyncStatusChanged(BmContext ctx, ContainerDescriptor cd, String subject) {
	}

	@Override
	public void onAclChanged(BmContext ctx, ContainerDescriptor cd, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		hierarchyOp(ctx, cd, hier -> {

			IDirectory dir = ctx.su().provider().instance(IDirectory.class, cd.domainUid);

			Set<AccessControlEntry> concernedSubjects = Sets.symmetricDifference(new HashSet<>(previous),
					new HashSet<>(current));

			Map<Boolean, List<AccessControlEntry>> partitioned = concernedSubjects.stream()
					.collect(Collectors.partitioningBy(s -> previous.contains(s)));

			Set<String> added = expand(dir, cd.domainUid,
					partitioned.get(Boolean.FALSE).stream().map(entry -> entry.subject).collect(Collectors.toList()));
			Set<String> removed = expand(dir, cd.domainUid,
					partitioned.get(Boolean.TRUE).stream().map(entry -> entry.subject).collect(Collectors.toList()));

			Set<String> subjects = Stream.concat(added.stream().map(subject -> subject + "," + "ADD"),
					removed.stream().map(subject -> subject + "," + "REMOVE")).collect(Collectors.toSet());

			JsonObject notif = new JsonObject() //
					.put("subjects", new JsonArray(new ArrayList<>(subjects))) //
					.put("domain", cd.domainUid) //
					.put("uid", cd.uid);

			VertxPlatform.eventBus().publish(Topic.MAPI_PF_ACL_UPDATE, notif);
		});
	}

	private Set<String> expand(IDirectory dir, String domain, List<String> subject) {
		if (subject.isEmpty()) {
			return Collections.emptySet();
		}
		ListResult<ItemValue<DirEntry>> search = dir
				.search(DirEntryQuery.filterEntryUid(subject.toArray(new String[0])));
		return search.values.stream().flatMap(collectMembers(domain)).collect(Collectors.toSet());
	}

	private Function<ItemValue<DirEntry>, Stream<String>> collectMembers(String domain) {
		return dirEntry -> {
			Set<String> members = new HashSet<>();
			if (dirEntry.value.kind != Kind.GROUP) {
				members.add(dirEntry.uid);
			} else {
				IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IGroup.class, domain);
				members.addAll(groupService.getExpandedUserMembers(dirEntry.uid).stream().map(m -> m.uid)
						.collect(Collectors.toSet()));
			}
			return Stream.of(members.toArray(new String[0]));
		};
	}

}
