/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IMessageBodyTierChange;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTask;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskFilter;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskStatus;
import net.bluemind.system.api.hot.upgrade.IHotUpgrade;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class MessageBodyTierChangeSysconfObserver
		implements ISystemConfigurationObserver, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(MessageBodyTierChangeSysconfObserver.class);

	/*
	 * We don't reprocess the queue on archiveSizeTreshold changes, because we can't
	 * figure out in what tier a message currently is. We can't filter the tierMove
	 * requests and would need to reprocess the whole spool, which is not
	 * reasonable.
	 */
	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf current) throws ServerFault {
		ArchiveKind previousArchiveKind = ArchiveKind.fromName(previous.stringValue(SysConfKeys.archive_kind.name()));
		ArchiveKind currentArchiveKind = ArchiveKind.fromName(current.stringValue(SysConfKeys.archive_kind.name()));

		if (previousArchiveKind != null && previousArchiveKind.supportsHsm()
				&& (currentArchiveKind == null || !currentArchiveKind.supportsHsm())) {
			// Archive kind is not cyrus anymore: truncate the storage tier move queue
			logger.info("archiveKind changed from {} to {}, no support for HSM tier move: flushing all queues",
					previousArchiveKind, currentArchiveKind);
			removeAllTierMoves();
			return;
		}

		Integer previousArchiveDays = previous.integerValue(SysConfKeys.archive_days.name(), 0);
		Integer currentArchiveDays = current.integerValue(SysConfKeys.archive_days.name(), 0);

		// Beyond this point, we are not interrested if the storage does not supports
		// HSM, or the archiveDays is 0
		if (currentArchiveKind == null || !currentArchiveKind.supportsHsm() || currentArchiveDays == 0) {
			return;
		}

		if (previousArchiveKind != null && !previousArchiveKind.supportsHsm()) {
			logger.info("archiveKind changed from {} to {} with archiveDays = {}: queueing tierMoves",
					previousArchiveKind, currentArchiveKind, currentArchiveDays);
			requeueAllTierMoves();
			return;
		}

		if (!previousArchiveDays.equals(currentArchiveDays)) {
			// We need to recalculate the storageTier change date
			logger.info("archiveDays changed from {} days to {} days: queueing tierMoves", previousArchiveDays,
					currentArchiveDays);
			requeueAllTierMoves();
		}
	}

	private void requeueAllTierMoves() {
		Topology.get().all(TagDescriptor.bm_pgsql_data.getTag()).stream().map(iv -> tierChangeService(iv.uid))
				.forEach(svc -> {
					svc.truncate();
					svc.requeueAllTierMoves();
				});
	}

	private void removeAllTierMoves() {
		Topology.get().all(TagDescriptor.bm_pgsql_data.getTag()).forEach(iv -> tierChangeService(iv.uid).truncate());
	}

	private IMessageBodyTierChange tierChangeService(String serverUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMessageBodyTierChange.class,
				serverUid);
	}

	/*
	 * This check will prevent systemConf modifications while a hotupgrade is un
	 * progress, because we don't want the administrator to requeue things, which
	 * requires an index on t_message_body, only created in a hotupgrade.
	 * (t_message_body(created, guid))
	 */
	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);
		List<String> parameters = Arrays.asList(SysConfKeys.archive_kind.name(), SysConfKeys.archive_days.name(),
				SysConfKeys.archive_size_threshold.name());
		IHotUpgrade hotupgradeService;
		try {
			hotupgradeService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IHotUpgrade.class);
		} catch (ServerFault sf) {
			logger.info("HSM: no sysconf validation: IHotUpgrade service is not installed: {}", sf.getMessage());
			return;
		}
		var mandatoryHotUpgradesNotFinished = hotupgradeService.list(HotUpgradeTaskFilter
				.filter(HotUpgradeTaskStatus.FAILURE, HotUpgradeTaskStatus.PLANNED).onlyMandatory(true));
		if (mandatoryHotUpgradesNotFinished.isEmpty()) {
			// All hotupgrades finished, nothing to worry about
			return;
		}
		for (String sysconfKey : modifications.keySet()) {
			if (parameters.contains(sysconfKey)) {
				String errorMessage = "system configuration update of " + sysconfKey
						+ " refused: mandatory hot upgrades in progress: " + mandatoryHotUpgradesNotFinished.stream()
								.map(HotUpgradeTask::toString).collect(Collectors.joining(","));
				logger.error(errorMessage);
				throw new ServerFault(errorMessage);
			}
		}
	}
}
