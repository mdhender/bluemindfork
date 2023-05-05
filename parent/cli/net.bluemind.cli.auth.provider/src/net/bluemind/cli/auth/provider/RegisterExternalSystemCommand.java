/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.auth.provider;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.IExternalSystem;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "openid-register-provider", description = "Register an OpenID provider system")
public class RegisterExternalSystemCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("auth-provider");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RegisterExternalSystemCommand.class;
		}
	}

	private class ProviderParameters {
		public final String identifier;
		public final String authEndpoint;
		public final String tokenEndpoint;
		public final String clientId;
		public final String clientSecret;

		public ProviderParameters(String identifier, String authEndpoint, String tokenEndpoint, String clientId,
				String clientSecrets) {
			this.identifier = identifier;
			this.authEndpoint = authEndpoint;
			this.tokenEndpoint = tokenEndpoint;
			this.clientId = clientId;
			this.clientSecret = clientSecrets;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = { "--domain" }, description = "Domain")
	public String domain;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@ArgGroup(exclusive = false)
		ScopeCmd cmd;
		@ArgGroup(exclusive = false)
		ScopeFile file;
	}

	private static class ScopeCmd {
		@Option(required = true, names = { "--identifier" }, description = "External system identifier")
		public String identifier;

		@Option(required = true, names = { "--auth-endpoint" }, description = "Open ID Connect auth endpoint")
		public String authEndpoint;

		@Option(required = true, names = { "--token-endpoint" }, description = "Open ID Connect token endpoint")
		public String tokenEndpoint;

		@Option(required = true, names = { "--client-id" }, description = "Application client ID")
		public String clientId;

		@Option(required = true, names = { "--client-secret" }, description = "Application client secret")
		public String clientSecret;
	}

	private static class ScopeFile {
		@Option(required = true, names = { "--file" }, description = "Import from file (ignores all other parameters)")
		public Path config;
	}

	@Override
	public void run() {
		ItemValue<Domain> domainItem = cliUtils.getDomain(domain)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));

		ProviderParameters providerParameter = Optional
				.ofNullable(scope.cmd).map(scope -> new ProviderParameters(scope.identifier, scope.authEndpoint,
						scope.tokenEndpoint, scope.clientId, scope.clientSecret))
				.orElseGet(this::providerParametersFromFile);

		IExternalSystem extSystemService = ctx.adminApi().instance(IExternalSystem.class);
		ExternalSystem externalSystem = extSystemService.getExternalSystem(providerParameter.identifier);

		if (externalSystem == null) {
			throw new CliException("External system " + providerParameter.identifier + " does not exist");
		}

		if (!externalSystem.authKind.name().startsWith("OPEN_ID")) {
			throw new CliException("External system " + providerParameter.identifier + " does not support OpenID");
		}

		String endpointKey = providerParameter.identifier + "_endpoint";
		String applicationIdKey = providerParameter.identifier + "_appid";
		String applicationSecretKey = providerParameter.identifier + "_secret";
		String tokenEndpointKey = providerParameter.identifier + "_tokenendpoint";

		domainItem.value.properties.put(endpointKey, providerParameter.authEndpoint);
		domainItem.value.properties.put(applicationIdKey, providerParameter.clientId);
		domainItem.value.properties.put(applicationSecretKey, providerParameter.clientSecret);
		domainItem.value.properties.put(tokenEndpointKey, providerParameter.tokenEndpoint);

		ctx.adminApi().instance(IDomains.class).update(domainItem.uid, domainItem.value);

		ctx.info("Configuration saved for system {} on domain {}", providerParameter.identifier, domain);
	}

	private ProviderParameters providerParametersFromFile() {
		ctx.info("Importing config from file {}", scope.file.config);

		try {
			Properties props = new Properties();
			props.load(new FileReader(scope.file.config.toFile()));

			ProviderParameters providerParameters = new ProviderParameters(props.getProperty("identifier"),
					props.getProperty("auth-endpoint"), props.getProperty("token-endpoint"),
					props.getProperty("client-id"), props.getProperty("client-secret"));

			ctx.info("Domain: {}", domain);
			ctx.info("Identifier: {}", providerParameters.identifier);
			ctx.info("Auth endpoint: {}", providerParameters.authEndpoint);
			ctx.info("Token endpoint: {}", providerParameters.tokenEndpoint);
			ctx.info("Client Id: {}", providerParameters.clientId);
			ctx.info("Client secret: {}", providerParameters.clientSecret);

			return providerParameters;
		} catch (Exception e) {
			throw new CliException(e);
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}
}
