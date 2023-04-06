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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.oauth;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.IExternalSystem;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "openid-system", description = "Register an external OpenID system")
public class RegisterExternalSystemCommand implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("oauth");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RegisterExternalSystemCommand.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = { "--domain" }, description = "Domain")
	public String domain;

	@Option(required = false, names = { "--identifier" }, description = "External system identifier")
	public String identifier;

	@Option(required = false, names = { "--auth-endpoint" }, description = "Open ID Connect auth endpoint")
	public String authEndpoint;

	@Option(required = false, names = { "--token-endpoint" }, description = "Open ID Connect token endpoint")
	public String tokenEndpoint;

	@Option(required = false, names = { "--client-id" }, description = "Application client ID")
	public String clientId;

	@Option(required = false, names = { "--client-secret" }, description = "Application client secret")
	public String clientSecret;

	@Option(required = false, names = { "--file" }, description = "Import from file (ignores all other parameters)")
	public Path config;

	@Override
	public void run() {

		ItemValue<Domain> domainItem = cliUtils.getDomain(domain)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));

		if (config != null) {
			ctx.info("Importing config from file {}", config);
			Properties props = new Properties();
			try {
				props.load(new FileReader(config.toFile()));
				domain = props.getProperty("domain");
				identifier = props.getProperty("identifier");
				authEndpoint = props.getProperty("auth-endpoint");
				tokenEndpoint = props.getProperty("token-endpoint");
				clientId = props.getProperty("client-id");
				clientSecret = props.getProperty("client-secret");
				ctx.info("Domain: {}", domain);
				ctx.info("Identifier: {}", identifier);
				ctx.info("Auth endpoint: {}", authEndpoint);
				ctx.info("Token endpoint: {}", tokenEndpoint);
				ctx.info("Client Id: {}", clientId);
				ctx.info("Client secret: {}", clientSecret);
			} catch (Exception e) {
				throw new CliException(e);
			}
		}

		IExternalSystem extSystemService = ctx.adminApi().instance(IExternalSystem.class);
		ExternalSystem externalSystem = extSystemService.getExternalSystem(identifier);

		if (externalSystem == null) {
			throw new CliException("External system " + identifier + " does not exist");
		}

		if (!externalSystem.authKind.name().startsWith("OPEN_ID")) {
			throw new CliException("External system " + identifier + " does not support OpenID");
		}

		String endpointKey = identifier + "_endpoint";
		String applicationIdKey = identifier + "_appid";
		String applicationSecretKey = identifier + "_secret";
		String tokenEndpointKey = identifier + "_tokenendpoint";

		IDomainSettings settings = ctx.adminApi().instance(IDomainSettings.class, domainItem.uid);
		Map<String, String> domainSettings = settings.get();

		domainSettings.put(endpointKey, authEndpoint);
		domainSettings.put(applicationIdKey, clientId);
		domainSettings.put(applicationSecretKey, clientSecret);
		domainSettings.put(tokenEndpointKey, tokenEndpoint);

		settings.set(domainSettings);

		ctx.info("Configuration saved for system {} on domain {}", identifier, domain);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

}
