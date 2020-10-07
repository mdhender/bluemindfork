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

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.RestoreDefinition;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.api.RetentionPolicy;
import net.bluemind.dataprotect.persistence.DataProtectGenerationStore;
import net.bluemind.dataprotect.persistence.GenerationWriter;
import net.bluemind.dataprotect.persistence.RetentionPolicyStore;
import net.bluemind.dataprotect.service.IRestoreActionProvider;
import net.bluemind.role.api.BasicRoles;

public class DPService implements IDataProtect {

	private static final Logger logger = LoggerFactory.getLogger(DPService.class);
	private final BmContext ctx;
	private final DataProtectGenerationStore dpgStore;
	private final RetentionPolicyStore rpStore;
	private final List<RestoreOperation> restoreOps;
	private List<IRestoreActionProvider> restoreProviders;
	private RBACManager rbac;

	public DPService(BmContext context, List<RestoreOperation> ops, List<IRestoreActionProvider> providers) {
		this.ctx = context;
		rbac = RBACManager.forContext(ctx);
		logger.debug("Built with ctx {}", ctx);
		this.dpgStore = new DataProtectGenerationStore(context.getDataSource());
		this.rpStore = new RetentionPolicyStore(context.getDataSource());
		this.restoreOps = ops;
		this.restoreProviders = providers;
	}

	protected DataProtectGenerationStore getStore() {
		return dpgStore;
	}

	@Override
	public List<DataProtectGeneration> getAvailableGenerations() throws ServerFault {
		checkAccess();
		try {
			return dpgStore.getGenerations();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public TaskRef getContent(String generationId) throws ServerFault {
		checkAccess();
		logger.info("Access is fine for {} loading gen {}", ctx, generationId);
		List<DataProtectGeneration> generations = getAvailableGenerations();
		DataProtectGeneration gen = null;
		int genId = Integer.parseInt(generationId);
		for (DataProtectGeneration dpg : generations) {
			if (genId == dpg.id) {
				gen = dpg;
				break;
			}
		}
		if (gen == null) {
			throw new ServerFault("Generation " + generationId + " not found.");
		}
		List<PartGeneration> parts = gen.parts;
		Optional<PartGeneration> part = parts.stream().filter(p -> "directory".equals(p.datatype)).findFirst();

		if (!part.isPresent()) {
			throw new ServerFault("directory part is missing from generation " + generationId);
		}

		LoadGenerationTask lgt = new LoadGenerationTask(ctx, part.get(), parts);
		TaskRef ret = ctx.provider().instance(ITasksManager.class).run(lgt);
		return ret;
	}

	@Override
	public List<RestoreOperation> getRestoreCapabilities() throws ServerFault {
		checkAccess();
		return restoreOps;
	}

	@Override
	public List<RestoreOperation> getRestoreCapabilitiesByTags(List<String> tags) throws ServerFault {
		checkAccess();
		return restoreOps.stream().filter(r -> {
			if (null != r.requiredTag) {
				return tags.contains(r.requiredTag);
			}
			return true;
		}).collect(Collectors.toList());
	}

	@Override
	public TaskRef run(RestoreDefinition restoreDefinition) throws ServerFault {
		ParametersValidator.notNull(restoreDefinition);
		ParametersValidator.notNull(restoreDefinition.item);
		ParametersValidator.notNull(restoreDefinition.item.domainUid);
		ParametersValidator.notNull(restoreDefinition.restoreOperationIdenfitier);

		rbac.forDomain(restoreDefinition.item.domainUid).check(BasicRoles.ROLE_MANAGE_RESTORE);
		List<DataProtectGeneration> generations = getAvailableGenerations();
		DataProtectGeneration dataSource = null;
		for (DataProtectGeneration dpg : generations) {
			if (dpg.id == restoreDefinition.generation) {
				dataSource = dpg;
				break;
			}
		}

		if (dataSource == null) {
			throw new ServerFault(String.format("data generation with id %s not found", restoreDefinition.generation),
					ErrorCode.NOT_FOUND);
		}

		IRestoreActionProvider matchingProvider = null;
		RestoreOperation mathingOp = null;
		for (IRestoreActionProvider prov : restoreProviders) {
			List<RestoreOperation> ops = prov.operations();
			for (RestoreOperation rop : ops) {
				if (rop.identifier.equals(restoreDefinition.restoreOperationIdenfitier)) {
					matchingProvider = prov;
					mathingOp = rop;
					break;
				}
			}
		}

		if (matchingProvider == null) {
			throw new ServerFault(
					String.format("No restore provider found for %s", restoreDefinition.restoreOperationIdenfitier),
					ErrorCode.NOT_FOUND);
		}

		return matchingProvider.run(mathingOp, dataSource, restoreDefinition.item);
	}

	@Override
	public TaskRef forget(int generationId) throws ServerFault {
		rbac.check(BasicRoles.ROLE_DATAPROTECT);
		DataProtectGeneration dpg = null;
		List<DataProtectGeneration> gens = getAvailableGenerations();
		for (DataProtectGeneration dp : gens) {
			if (dp.id == generationId) {
				dpg = dp;
				break;
			}
		}
		if (dpg == null) {
			throw new ServerFault("Generation " + generationId + " not found");
		}
		ForgetTask install = new ForgetTask(ctx, this, dpg);
		return ctx.provider().instance(ITasksManager.class).run(install);
	}

	@Override
	public RetentionPolicy getRetentionPolicy() throws ServerFault {
		rbac.check(BasicRoles.ROLE_DATAPROTECT);
		try {
			return rpStore.get();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void updatePolicy(RetentionPolicy rp) throws ServerFault {
		rbac.check(BasicRoles.ROLE_DATAPROTECT);
		rpStore.update(rp);
	}

	@Override
	public void syncWithFilesystem() throws ServerFault {
		rbac.check(BasicRoles.ROLE_SYSTEM_MANAGER);
		List<DataProtectGeneration> storedGenerations = GenerationWriter.readGenerationFiles();
		logger.info("rewriting generations using {} stored generations", storedGenerations.size());
		dpgStore.rewriteGenerations(storedGenerations);
	}

	@Override
	public TaskRef installFromGeneration(int generationId) throws ServerFault {
		rbac.check(BasicRoles.ROLE_SYSTEM_MANAGER);
		DataProtectGeneration dpg = null;
		List<DataProtectGeneration> gens = getAvailableGenerations();
		for (DataProtectGeneration dp : gens) {
			if (dp.id == generationId) {
				dpg = dp;
				break;
			}
		}
		if (dpg == null) {
			throw new ServerFault("Generation " + generationId + " not found");
		}
		InstallTask install = new InstallTask(ctx, dpg);
		return ctx.provider().instance(ITasksManager.class).run(install);
	}

	@Override
	public TaskRef saveAll() throws ServerFault {
		rbac.check(BasicRoles.ROLE_SYSTEM_MANAGER);
		logger.info("Backup TIME....");
		return ctx.provider().instance(ITasksManager.class).run(new SaveAllTask(ctx, this));
	}

	private void checkAccess() throws ServerFault {
		rbac.forDomain(ctx.getSecurityContext().getContainerUid()).check(BasicRoles.ROLE_MANAGE_RESTORE,
				BasicRoles.ROLE_DATAPROTECT);
	}

}
