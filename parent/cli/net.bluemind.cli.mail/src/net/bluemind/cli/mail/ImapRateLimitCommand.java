package net.bluemind.cli.mail;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueFactory;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.configfile.imap.ImapConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "imap-rate-limit", description = "Shows the number of indexed messages")
public class ImapRateLimitCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ImapRateLimitCommand.class;
		}
	}

	public enum Strategy {
		none, intime, behind // NOSONAR: lowercase 'cause picoli enum are case sensitive by defaults
	}

	private static final ConfigRenderOptions renderingOptions = ConfigRenderOptions.defaults().setJson(false)
			.setOriginComments(false).setComments(true).setFormatted(true);

	private CliContext ctx;

	@Option(required = false, names = "--strategy", description = "IMAP rate limit strategy, use 'none' to remove any limits (${COMPLETION-CANDIDATES})")
	public Strategy strategy;

	@Option(required = false, names = "--bypass", split = ",", description = "Remove IMAP rate limit for specific users (comma separated list of login@domain.internal)")
	public List<String> bypass;

	@Option(required = false, names = "--no-bypass", description = "Remove existing IMAP rate limit bypass for users")
	public boolean noBypass = false;

	@Option(required = false, names = "--reset", description = "Reset IMAP rate limit configuration to defaults")
	public boolean reset = false;

	@Override
	public void run() {
		String configPath = ImapConfig.OVERRIDE_PATH;
		Config config = loadConfigurationOverrides(configPath);
		config = updateConfiguration(config);
		writeConfigurationOvverrides(configPath, config);
		reloadBmCoreService();
	}

	private Config loadConfigurationOverrides(String path) {
		File local = new File(path);
		Config config = ConfigFactory.empty();
		if (local.exists()) {
			try {
				config = ConfigFactory.parseFile(local);
				ctx.info("Current IMAP configuration overrides:\n{}", config.root().render(renderingOptions));
			} catch (ConfigException e) {
				ctx.error("Existing file in '/etc/bm/imap.conf' was invalid and is being ignored: {}", e.getMessage());
			}
		} else {
			ctx.info("No existing IMAP configuration overrides", config.root().render(renderingOptions));
		}
		return config;
	}

	private Config updateConfiguration(Config config) {
		if (reset) {
			return config.withoutPath(ImapConfig.Throughput.KEY);
		}

		if (strategy != null) {
			config = config.withValue(ImapConfig.Throughput.STRATEGY, ConfigValueFactory.fromAnyRef(strategy.name()));
		}

		if (noBypass) {
			config = config.withValue(ImapConfig.Throughput.BYPASS, ConfigValueFactory.fromIterable(emptyList()));
		} else if (bypass != null) {
			config = config.withValue(ImapConfig.Throughput.BYPASS, ConfigValueFactory.fromIterable(bypass));
		}

		return config;
	}

	private void writeConfigurationOvverrides(String path, Config config) {
		try {
			String configToWrite = config.root().render(renderingOptions);
			Files.write(Path.of(path), configToWrite.getBytes());
			ctx.info("New IMAP configuration overrides:\n{}", configToWrite);
		} catch (IOException e) {
			ctx.error("Unable to write file to {}", path, e);
		}
	}

	private void reloadBmCoreService() {
		String reloadCommand = "systemctl reload bm-core";
		try {
			Process process = Runtime.getRuntime().exec(reloadCommand);
			int exitVal = process.waitFor();
			if (exitVal == 0) {
				ctx.info("'{}': success", reloadCommand);
			} else {
				String errorOutput = process.errorReader().lines().collect(Collectors.joining("\n"));
				ctx.error("'{}': failed with message '{}'", reloadCommand, errorOutput);
			}
		} catch (IOException e) {
			ctx.error("Failed to run '{}'", reloadCommand, e);
		} catch (InterruptedException e) {
			ctx.error("Got interrupted while waiting for '{}'", reloadCommand, e);
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
