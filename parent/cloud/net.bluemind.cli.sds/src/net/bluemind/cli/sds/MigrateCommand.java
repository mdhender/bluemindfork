package net.bluemind.cli.sds;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskFilter;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskStatus;
import net.bluemind.system.api.hot.upgrade.IHotUpgrade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "migrate", description = "Migrates messages from cyrus archive partition to object store")
public class MigrateCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	private static final Path root = Paths.get("/var/spool/bm-cli/");
	static {
		root.toFile().mkdirs();
	}

	public MigrateCommand() {
		// OK
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Parameters(paramLabel = "<file>", description = "a Json file which contains one or multiple key-value pairs")
	public Path file = null;

	@Option(required = true, names = "--format", description = "a Json or Properties file which contains one or multiple key-value pairs. Format value : <json|properties>")
	public String format = null;

	@Option(names = "--workers", description = "run with X workers")
	public int workers = 32;

	@Option(names = "--force", description = "Force running, even if we are not happy about current SystemConfiguration")
	public boolean force = false;

	@Option(names = "--no-emails", description = "Migrate emails from cyrus archives to SDS", negatable = true)
	public boolean migrateEmails = true;

	@Option(names = "--full-resync", description = "Fully resync, without checking the cache", negatable = true)
	public boolean fullResync = false;

	@Option(names = "--no-filehosting", description = "Migrate filehosting to SDS", negatable = true)
	public boolean migrateFileHosting = true;

	@Option(names = "--filehosting-root", description = "Where the migration should search for filehosting files")
	public Path filehostingRoot = Paths.get("/var/spool/bm-filehosting");

	@Option(names = "--document-root", description = "Where the migration should search for document files")
	public Path documentRoot = Paths.get("/var/spool/bm-docs");

	private Map<String, String> jsonFileToMap(Path filepath) {
		Map<String, String> map = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			map = mapper.readValue(filepath.toFile(), new TypeReference<Map<String, String>>() {
			});
		} catch (Exception ex) {
			ctx.error(ex.getMessage());
		}
		return map;
	}

	private Map<String, String> propertiesFileToMap(Path filepath) {
		Map<String, String> map = Collections.emptyMap();
		Properties prop = new Properties();
		try (InputStream input = Files.newInputStream(filepath)) {
			prop.load(input);
			map = prop.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Object::toString));
		} catch (Exception ex) {
			ctx.error(ex.getMessage());
		}

		return map;
	}

	private boolean hasMandatoryUpgradesNotSuccessfully() {
		IHotUpgrade hotUpgradeApi = ctx.adminApi().instance(IHotUpgrade.class);
		return hotUpgradeApi.list(HotUpgradeTaskFilter.all()).stream() //
				.filter(hotupgrade -> hotupgrade.mandatory) //
				.filter(hotupgrade -> !"repair".equals(hotupgrade.operation)) //
				.filter(hotupgrade -> !"elasticAliasMove".equals(hotupgrade.operation)) //
				.filter(hotupgrade -> !"init-conversation-db".equals(hotupgrade.operation)) //
				.filter(hotupgrade -> hotupgrade.status != HotUpgradeTaskStatus.SUCCESS) //
				.count() > 0;
	}

	@Override
	public void run() {
		Map<String, String> conf;

		if (hasMandatoryUpgradesNotSuccessfully()) {
			ctx.error(
					"NO: mandatory hotupgrades are not all in status SUCCESS. Cannot continue. Please check hot-upgrades");
			System.exit(10);
		}

		if (Files.isReadable(file)) {
			if (format.equalsIgnoreCase("json")) {
				conf = jsonFileToMap(file);
			} else if (format.equalsIgnoreCase("properties")) {
				conf = propertiesFileToMap(file);
			} else {
				ctx.error(String.format("format unrecognized: %s", format));
				return;
			}
		} else {
			ctx.error(String.format("%s not found or is not readable", file));
			conf = Collections.emptyMap();
		}

		if (conf.isEmpty()) {
			ctx.error("Configuration file is required");
			System.exit(1);
		}

		if (migrateFileHosting) {
			ctx.info("Migrating FileHosting...");
			FileHostingMigrator fileHostingMigrator = new FileHostingMigrator(ctx, workers, conf);
			try {
				if (filehostingRoot.toFile().exists()) {
					fileHostingMigrator.migrateFileHosting(filehostingRoot);
				} else {
					ctx.info("file hosting root {} does not exists, launched on a backend?", filehostingRoot);
				}
				if (documentRoot.toFile().exists()) {
					fileHostingMigrator.migrateDocuments(documentRoot);
				} else {
					ctx.info("document root {} does not exists, launched on a backend?", documentRoot);
				}
			} catch (IOException e) {
				ctx.error(e.getMessage());
				e.printStackTrace();
				System.exit(2);
			}
		}

		if (migrateEmails) {
			ctx.info("Migrating emails...");
			EmailMigrator emailMigrator = new EmailMigrator(ctx, workers, root, conf);
			if (fullResync) {
				emailMigrator.clearCache();
			}
			emailMigrator.migrateEmails();
		}
	}

	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("sds");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MigrateCommand.class;
		}
	}
}
