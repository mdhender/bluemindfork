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

package net.bluemind.dataprotect.service.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.dataprotect.api.GenerationContent;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.hollow.datamodel.consumer.DirectoryDeserializer;
import net.bluemind.directory.hollow.datamodel.consumer.Email;
import net.bluemind.domain.api.Domain;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class LoadGenerationTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(LoadGenerationTask.class);

	private final PartGeneration directory;
	private final List<PartGeneration> parts;
	private BmContext ctx;

	private RBACManager rbac;

	public LoadGenerationTask(BmContext ctx, PartGeneration directory, List<PartGeneration> parts) {
		this.rbac = RBACManager.forContext(ctx);
		this.directory = directory;
		this.parts = parts;
		this.ctx = ctx;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		GenerationContent gc = new GenerationContent();
		gc.generationId = directory.generationId;
		IDataProtect backupApi = ctx.provider().instance(IDataProtect.class);
		List<String> partTags = parts.stream().map(p -> {
			return p.tag;
		}).collect(Collectors.toList());
		gc.capabilities = backupApi.getRestoreCapabilitiesByTags(partTags);

		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = sp.instance(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> server = serverApi.getComplete(directory.server);

		String path = String.format(
				"/var/backups/bluemind/dp_spool/rsync/%s/bm/core/%d/var/backups/bluemind/work/directory/",
				server.value.ip, directory.id);

		File dir = new File(path);
		if (!dir.exists()) {
			throw new ServerFault("Fail to fetch directory data");
		}

		gc.entries = new ArrayList<ItemValue<DirEntry>>();
		gc.domains = new ArrayList<ItemValue<Domain>>();

		File[] files = dir.listFiles(File::isDirectory);
		for (File snapshot : files) {
			DirectoryDeserializer dd = new DirectoryDeserializer(snapshot);

			String domainUid = snapshot.getName();

			if (!rbac.forDomain(domainUid).can(BasicRoles.ROLE_MANAGE_RESTORE, BasicRoles.ROLE_DATAPROTECT)) {
				continue;
			}

			Domain dom = new Domain();
			dom.name = domainUid;
			gc.domains.add(ItemValue.create(domainUid, dom));

			dd.all().stream().forEach(abRecord -> {
				DirEntry de = new DirEntry();
				de.entryUid = abRecord.getUid();
				de.displayName = abRecord.getName();
				de.kind = Kind.valueOf(abRecord.getKind().getValue());
				// ui try to guess the domainUid with the path
				de.path = domainUid + "/";
				if (abRecord.getDataLocation() != null) {
					de.dataLocation = abRecord.getDataLocation().getServer().getValue();
				}
				if (abRecord.getEmails() != null && !abRecord.getEmails().isEmpty()) {
					de.email = abRecord.getEmails().stream().filter(Email::getIsDefault).findFirst().get().getAddress();
				}

				gc.entries.add(ItemValue.create(de.entryUid, de));
			});
		}

		logger.info("Sending generation with {} capabilities", gc.capabilities.size());
		monitor.end(true, "restored", JsonUtils.asString(gc));

	}

}
