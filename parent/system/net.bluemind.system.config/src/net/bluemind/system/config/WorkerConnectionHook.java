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
package net.bluemind.system.config;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.nginx.NginxService;

public class WorkerConnectionHook implements ISystemConfigurationObserver, ISystemConfigurationValidator {
	private static Logger logger = LoggerFactory.getLogger(WorkerConnectionHook.class);
	private Optional<NginxService> nginxService;

	public WorkerConnectionHook() {
		this.nginxService = Optional.empty();
	}

	public WorkerConnectionHook(NginxService nginxService) {
		this.nginxService = Optional.of(nginxService);
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String workerConnections = conf.values.get(SysConfKeys.nginx_worker_connections.name());

		if ((Strings.isNullOrEmpty(workerConnections)
				&& Strings.isNullOrEmpty(previous.values.get(SysConfKeys.nginx_worker_connections.name())))
				|| Strings.nullToEmpty(workerConnections).equals(
						Strings.nullToEmpty(previous.values.get(SysConfKeys.nginx_worker_connections.name())))) {
			return;
		}

		logger.info("System configuration {} has been updated", SysConfKeys.nginx_worker_connections.name());
		nginxService.orElse(new NginxService()).updateWorkerConnection(workerConnections);
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (modifications.containsKey(SysConfKeys.nginx_worker_connections.name())) {
			try {
				Integer.parseInt(modifications.get(SysConfKeys.nginx_worker_connections.name()));
			} catch (NumberFormatException nfe) {
				throw new ServerFault(
						String.format("%s must be a valid integer", SysConfKeys.nginx_worker_connections.name()),
						ErrorCode.INVALID_PARAMETER);
			}
		}
	}
}
