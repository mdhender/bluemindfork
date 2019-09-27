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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.integrity.check;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.system.validation.IProductValidator;

public class CyrusExtraDirectoriesValidation implements IProductValidator {

	private static final Logger logger = LoggerFactory.getLogger(CyrusExtraDirectoriesValidation.class);

	@Override
	public String getName() {
		return "CyrusExtraDirectories";
	}

	@Override
	public ValidationResult validate() {
		BmContext adminCtx = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		List<ItemValue<Domain>> domains = adminCtx.provider().instance(IDomains.class).all().stream()
				.filter(d -> !d.uid.equals("global.virt")).collect(Collectors.toList());

		CyrusFilesystemCheck fs = new CyrusFilesystemCheck(adminCtx, domains);
		List<ItemValue<Server>> backends = Topology.get().nodes().stream()
				.filter(s -> s.value.tags.contains("mail/imap")).collect(Collectors.toList());
		CompletableFuture<?>[] allChecks = new CompletableFuture[backends.size()];
		AtomicInteger failures = new AtomicInteger();
		StringBuilder report = new StringBuilder();
		for (int i = 0; i < backends.size(); i++) {
			final ItemValue<Server> back = backends.get(i);
			CompletableFuture<List<String>> checkResult = fs.check(back);
			allChecks[i] = checkResult.whenComplete((extraDirs, ex) -> {
				if (ex != null) {
					logger.error(ex.getMessage(), ex);
					failures.incrementAndGet();
					report.append(String.format("%s: %s\n", back.value.address(), ex.getMessage()));
				} else if (!extraDirs.isEmpty()) {
					failures.incrementAndGet();
					report.append(String.format("%s: %s extra dir(s):\n", back.value.address(), extraDirs.size()));
					for (String extra : extraDirs) {
						report.append(String.format("  * %s\n", extra));
					}
				}
			});
		}
		return failures.intValue() == 0 ? ValidationResult.valid() : ValidationResult.notValid(report.toString());
	}

}
