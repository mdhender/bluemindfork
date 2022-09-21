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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.GenerationContent;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.role.api.BasicRoles;

public class PRA extends BlockingServerTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(LoadGenerationTask.class);

	private final PartGeneration pgPart;
	private final List<PartGeneration> parts;
	private final String target;
	private BmContext ctx;
	private DataProtectGeneration dpGeneration;

	private RBACManager rbac;

	public PRA(BmContext ctx, DataProtectGeneration dpGeneration, PartGeneration pgPart, List<PartGeneration> parts,
			String target) {
		this.rbac = RBACManager.forContext(ctx);
		this.pgPart = pgPart;
		this.parts = parts;
		this.dpGeneration = dpGeneration;
		this.target = target;
		this.ctx = ctx;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {

		GenerationContent gc = new GenerationContent();
		gc.generationId = pgPart.generationId;

		try (BackupDataProvider backupProvider = new BackupDataProvider(target, SecurityContext.SYSTEM, monitor)) {
			IDataProtect backupApi = ctx.provider().instance(IDataProtect.class);

			String log = "Starting restore from part " + pgPart.tag + " " + pgPart.server;
			monitor.begin(2, log);
			logger.info(log);

			IServiceProvider sp = backupProvider.DIRECTORY(pgPart, dpGeneration.blueMind).provider();
			IDomains domainApi = sp.instance(IDomains.class, InstallationId.getIdentifier());
			gc.domains = domainApi.all().stream()
					.filter(d -> rbac.forDomain(d.uid).can(BasicRoles.ROLE_MANAGE_RESTORE, BasicRoles.ROLE_DATAPROTECT))
					.collect(Collectors.toList());
			gc.entries = new LinkedList<>();
			for (ItemValue<Domain> domain : gc.domains) {
				IDirectory dirApi = sp.instance(IDirectory.class, domain.uid);
				DirEntryQuery all = DirEntryQuery.all();
				all.hiddenFilter = false;
				ListResult<ItemValue<DirEntry>> allEntries = dirApi.search(all);

				gc.entries.addAll(allEntries.values);
			}
			List<String> partTags = parts.stream().map(p -> p.tag).collect(Collectors.toList());
			gc.capabilities = backupApi.getRestoreCapabilitiesByTags(partTags);

		}

		logger.info("Sending generation with {} capabilities.", gc.capabilities.size());
		monitor.end(true, "restored", JsonUtils.asString(gc));
	}

}