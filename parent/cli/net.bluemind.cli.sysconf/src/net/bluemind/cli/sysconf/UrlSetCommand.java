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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "url-set", description = "Set main or domain external-url and/or other-urls.")
public class UrlSetCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UrlSetCommand.class;
		}
	}

	private CliContext ctx;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@ArgGroup(exclusive = false)
		ScopeMain main;
		@ArgGroup(exclusive = false)
		ScopeDomain domain;
	}

	private static class ScopeMain {
		@Option(names = "--main-external-url", required = false, description = "Main external-url - Must not be empty (\"\").\nIf absent, keep previous value.\nEx: url.domain.tld")
		private String externalUrl;

		@Option(names = "--main-other-urls", required = false, split = ",", description = "Comma separated list of URLs, complements main external-url.\nEmpty (\"\") to remove main URLs except external-url.\nIf absent, keep previous value.\nEx: other1.url.domain.tld,other2.url.domain.tld")
		private String[] otherUrls;

		public void run(CliContext ctx) {
			Map<String, String> newSysconf = new HashMap<>();
			getExternalUrl().ifPresent(newSysconf::putAll);
			getOtherUrls().ifPresent(newSysconf::putAll);

			if (newSysconf.isEmpty()) {
				ctx.warn("No update needed, main URLs setting canceled");
				return;
			}

			SystemConf sysconf = ctx.adminApi().instance(ISystemConfiguration.class).getValues();

			ctx.info("Updating main URLs...");

			try {
				ctx.adminApi().instance(ISystemConfiguration.class).updateMutableValues(newSysconf);
			} catch (ServerFault sf) {
				throw new CliException("Updating main URLs failed: " + sf.getMessage());
			}

			updateReport(ctx, sysconf, newSysconf);
		}

		private void updateReport(CliContext ctx, SystemConf previousSysconf, Map<String, String> newSysconf) {
			if (newSysconf.containsKey(SysConfKeys.external_url.name())) {
				ctx.info(String.format("Main external-url updated\n was: '%s'\n updated to: '%s'",
						previousSysconf.stringValue(SysConfKeys.external_url.name()),
						newSysconf.get(SysConfKeys.external_url.name())));
			}

			if (newSysconf.containsKey(SysConfKeys.other_urls.name())) {
				Set<String> previous = Optional.ofNullable(previousSysconf.values.get(SysConfKeys.other_urls.name()))
						.map(ou -> Stream.of(ou.split(" ")).map(String::trim).filter(s -> !s.isEmpty())
								.collect(Collectors.toSet()))
						.orElseGet(() -> Collections.emptySet());

				Set<String> current = Optional
						.ofNullable(newSysconf.get(SysConfKeys.other_urls.name())).map(ou -> Stream.of(ou.split(" "))
								.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet()))
						.orElseGet(() -> Collections.emptySet());

				ctx.info(String.format("Main other URLs updated\n was: '%s'\n updated to: '%s'\n diff: +%s, -%s",
						previousSysconf.stringValue(SysConfKeys.other_urls.name()) == null ? ""
								: previousSysconf.values.get(SysConfKeys.other_urls.name()),
						newSysconf.get(SysConfKeys.other_urls.name()) == null ? ""
								: newSysconf.get(SysConfKeys.other_urls.name()),
						Sets.difference(current, previous), Sets.difference(previous, current)));
			}
		}

		private Optional<Map<String, String>> getExternalUrl() {
			if (externalUrl == null) {
				return Optional.empty();
			}

			String eu = externalUrl.trim();
			if (eu.isEmpty()) {
				throw new CliException("main-external-url must not be empty!");
			}

			Map<String, String> sysconf = new HashMap<>();
			sysconf.put(SysConfKeys.external_url.name(), eu);
			return Optional.of(sysconf);
		}

		private Optional<Map<String, String>> getOtherUrls() {
			if (otherUrls == null) {
				return Optional.empty();
			}

			String ou = Arrays.asList(otherUrls).stream().filter(Objects::nonNull).map(String::trim).distinct()
					.collect(Collectors.joining(" "));

			Map<String, String> sysconf = new HashMap<>();
			sysconf.put(SysConfKeys.other_urls.name(), ou.isEmpty() ? null : ou);
			return Optional.of(sysconf);
		}
	}

	private static class ScopeDomain {
		@Option(names = "--domain", required = true, description = "Target domain - must not be global.virt")
		private String domain;

		@ArgGroup(exclusive = false, multiplicity = "1")
		private ByDomainUrls urls;

		private static class ByDomainUrls {
			@Option(names = "--domain-external-url", required = false, description = "Domain external-url.\nEmpty (\"\") to remove domain external-url. Domain other URLs must be empty too.\nIf absent, keep previous value.\nEx: url.domain.tld")
			private String externalUrl;

			@Option(names = "--domain-other-urls", required = false, split = ",", description = "Comma separated list of URLs, complements domain external-url.\nDomain external-url must be set.\nEmpty (\"\") to remove URLs except external-url.\nIf absent, keep previous value.\nEx: other1.url.domain.tld,other2.url.domain.tld")
			private String[] otherUrls;
		}

		public void run(CliContext ctx) {
			ItemValue<Domain> d = new CliUtils(ctx).getDomain(domain).orElseThrow(() -> new CliException());

			if (d.value.global) {
				throw new CliException("Domain must not be global.virt domain!");
			}

			Map<String, String> newDomainSettings = new HashMap<>();
			getExternalUrl().ifPresent(newDomainSettings::putAll);
			getOtherUrls().ifPresent(newDomainSettings::putAll);

			if (newDomainSettings.isEmpty()) {
				ctx.warn(String.format("No update needed, domain '%s' URLs setting canceled", domain));
				return;
			}

			Map<String, String> domainSettings = ctx.adminApi().instance(IDomainSettings.class, d.uid).get();

			ctx.info(String.format("Updating domain '%s' URLs...", domain));

			try {
				ctx.adminApi().instance(IDomainSettings.class, d.uid)
						.set(updatedDomainSettings(domainSettings, newDomainSettings));
			} catch (ServerFault sf) {
				throw new CliException(String.format("Updating domain '%s' URLs failed: %s", domain, sf.getMessage()));
			}

			updateReport(ctx, domainSettings, newDomainSettings);
		}

		private Map<String, String> updatedDomainSettings(Map<String, String> domainSettings,
				Map<String, String> newDomainSettings) {
			Map<String, String> updatedDomainSettings = new HashMap<>(domainSettings);

			updateSetting(updatedDomainSettings, DomainSettingsKeys.external_url.name(), newDomainSettings);
			updateSetting(updatedDomainSettings, DomainSettingsKeys.other_urls.name(), newDomainSettings);

			return updatedDomainSettings;
		}

		private void updateSetting(Map<String, String> updatedDomainSettings, String key,
				Map<String, String> newDomainSettings) {
			if (!newDomainSettings.containsKey(key)) {
				return;
			}

			String value = newDomainSettings.get(key);
			if (value == null) {
				updatedDomainSettings.remove(key);
			} else {
				updatedDomainSettings.put(key, value);
			}
		}

		private void updateReport(CliContext ctx, Map<String, String> previous, Map<String, String> current) {
			if (current.containsKey(DomainSettingsKeys.external_url.name())) {
				ctx.info(String.format("Domain '%s' external-url updated\n was: '%s'\n updated to: '%s'", domain,
						previous.get(DomainSettingsKeys.external_url.name()) == null ? ""
								: previous.get(DomainSettingsKeys.external_url.name()),
						current.get(DomainSettingsKeys.external_url.name()) == null ? ""
								: current.get(DomainSettingsKeys.external_url.name())));
			}

			if (current.containsKey(SysConfKeys.other_urls.name())) {
				Set<String> prev = Optional.ofNullable(previous.get(DomainSettingsKeys.other_urls.name()))
						.map(ou -> Stream.of(ou.split(" ")).map(String::trim).filter(s -> !s.isEmpty())
								.collect(Collectors.toSet()))
						.orElseGet(() -> Collections.emptySet());

				Set<String> cur = Optional.ofNullable(current.get(DomainSettingsKeys.other_urls.name()))
						.map(ou -> Stream.of(ou.split(" ")).map(String::trim).filter(s -> !s.isEmpty())
								.collect(Collectors.toSet()))
						.orElseGet(() -> Collections.emptySet());

				ctx.info(String.format("Domain '%s' other URLs updated\n was: '%s'\n updated to: '%s'\n diff: +%s, -%s",
						domain,
						previous.get(DomainSettingsKeys.other_urls.name()) == null ? ""
								: previous.get(DomainSettingsKeys.other_urls.name()),
						current.get(DomainSettingsKeys.other_urls.name()) == null ? ""
								: current.get(DomainSettingsKeys.other_urls.name()),
						Sets.difference(cur, prev), Sets.difference(prev, cur)));
			}
		}

		private Optional<Map<String, String>> getExternalUrl() {
			if (urls.externalUrl == null) {
				return Optional.empty();
			}

			String eu = urls.externalUrl.trim();

			Map<String, String> sysconf = new HashMap<>();
			sysconf.put(DomainSettingsKeys.external_url.name(), eu.isEmpty() ? null : eu);
			return Optional.of(sysconf);
		}

		private Optional<Map<String, String>> getOtherUrls() {
			if (urls.otherUrls == null) {
				return Optional.empty();
			}

			String ou = Arrays.asList(urls.otherUrls).stream().filter(Objects::nonNull).map(String::trim).distinct()
					.collect(Collectors.joining(" "));

			Map<String, String> sysconf = new HashMap<>();
			sysconf.put(DomainSettingsKeys.other_urls.name(), ou.isEmpty() ? null : ou);
			return Optional.of(sysconf);
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Override
	public void run() {
		try {
			Optional.ofNullable(scope.main).ifPresent(g -> g.run(ctx));
			Optional.ofNullable(scope.domain).ifPresent(g -> g.run(ctx));
		} catch (CliException e) {
			if (!Strings.isNullOrEmpty(e.getMessage())) {
				ctx.error(e.getMessage());
			}
		}
	}
}