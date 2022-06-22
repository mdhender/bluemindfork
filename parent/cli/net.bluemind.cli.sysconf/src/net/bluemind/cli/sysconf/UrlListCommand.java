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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.sysconf;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "url-list", description = "List external-url and other-urls setup.\nList configurations for all domains by default")
public class UrlListCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UrlListCommand.class;
		}
	}

	protected CliContext ctx;

	@ArgGroup(exclusive = true)
	Scope scope;

	static class Scope {
		@Option(names = "--global", required = true)
		boolean globalDomain;
		@Option(names = "--domain", required = true)
		String domain;

		public Optional<ItemValue<Domain>> getDomain(CliContext ctx) {
			return new CliUtils(ctx).getDomain(globalDomain ? "global.virt" : domain);
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Override
	public void run() {
		Optional<Scope> scope = Optional.ofNullable(this.scope);

		scope.ifPresent(
				s -> s.getDomain(ctx).ifPresent(d -> ctx.info(new JsonArray(Arrays.asList(toJsonObject(d))).encode())));

		if (!scope.isPresent()) {
			ctx.info(new JsonArray(ctx.adminApi().instance(IDomains.class).all().stream().map(this::toJsonObject)
					.collect(Collectors.toList())).encode());
		}
	}

	private JsonObject toJsonObject(ItemValue<Domain> domain) {
		JsonObject jsonObject = new JsonObject().put("domainuid", domain.uid);

		String externalUrl = null;
		String otherUrls = null;
		if (domain.value.global) {
			SystemConf systemConf = ctx.adminApi().instance(ISystemConfiguration.class).getValues();
			externalUrl = systemConf.values.get(SysConfKeys.external_url.name());
			otherUrls = systemConf.values.get(SysConfKeys.other_urls.name());
		} else {
			Map<String, String> domainSettings = ctx.adminApi().instance(IDomainSettings.class, domain.uid).get();
			externalUrl = domainSettings.get(DomainSettingsKeys.external_url.name());
			otherUrls = domainSettings.get(DomainSettingsKeys.other_urls.name());
		}

		return jsonObject.put("external-url", externalUrl == null ? "" : externalUrl).put("other-urls",
				otherUrls == null ? "" : otherUrls);
	}
}