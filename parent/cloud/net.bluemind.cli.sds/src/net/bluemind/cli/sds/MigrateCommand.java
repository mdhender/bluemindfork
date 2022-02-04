package net.bluemind.cli.sds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "migrate", description = "Migrates messages from cyrus archive partition to object store")
public class MigrateCommand implements ICmdLet, Runnable {
	private static final String SDS_ENDPOINT_PUTOBJECT = "http://127.0.0.1:8091/sds";
	private static final String SDS_ENDPOINT_CONFIGURATION = "http://127.0.0.1:8091/configuration";

	private CliContext ctx;
	private AsyncHttpClient ahc;
	private CliUtils cliUtils;
	private final DB db;
	private final HTreeMap<Long, Integer> migrationMap;
	private static final AtomicInteger uploadCount = new AtomicInteger(0);

	private static final Path root = Paths.get("/var/spool/bm-cli/");
	static {
		root.toFile().mkdirs();
	}

	public MigrateCommand() {
		db = DBMaker.fileDB(root.resolve("sds-migrate.db").toAbsolutePath().toString()).transactionEnable()
				.fileMmapEnable().make();
		migrationMap = db.hashMap("migrate").keySerializer(Serializer.LONG).valueSerializer(Serializer.INTEGER)
				.createOrOpen();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		cliUtils = new CliUtils(ctx);
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

	@Option(names = "--cache", description = "Check the cache file before trying to push to SDS", negatable = true)
	public boolean cache = true;

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
			map = prop.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.toString()));
		} catch (Exception ex) {
			ctx.error(ex.getMessage());
		}

		return map;
	}

	@Override
	public void run() {
		if (!checkSysConf() && !force) {
			return;
		}

		ahc = new DefaultAsyncHttpClient();

		if (Files.isReadable(file)) {
			Map<String, String> map;
			if (format.equalsIgnoreCase("json")) {
				map = jsonFileToMap(file);
			} else if (format.equalsIgnoreCase("properties")) {
				map = propertiesFileToMap(file);
			} else {
				ctx.error(String.format("format unrecognized: %s", format));
				return;
			}
			try {
				updateSdsConfiguration(ahc, map);
			} catch (Exception e) {
				System.exit(1);
			}
		} else {
			ctx.error(String.format("%s not found or is not readable", file));
		}

		try {
			for (String domain : cliUtils.getDomainUids()) {
				try {
					String currentServer = new String(Files.readAllBytes(Paths.get("/etc/bm/server.uid")));
					CyrusPartition partition = CyrusPartition.forServerAndDomain(currentServer, domain);
					migrate(partition);
				} catch (IOException e) {
					ctx.error(e.getMessage());
					e.printStackTrace();
					System.exit(1);
				}
			}
		} finally {
			db.close();
		}
	}

	private boolean checkSysConf() {
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
		SystemConf sysConf = configurationApi.getValues();
		String archiveKind = sysConf.stringValue(SysConfKeys.archive_kind.name());
		int archiveSizeThreshold = sysConf.integerValue(SysConfKeys.archive_size_threshold.name());
		int archiveDays = sysConf.integerValue(SysConfKeys.archive_days.name());
		boolean ret = true;

		if ("cyrus".equals(archiveKind)) {
			if (archiveDays < 30) {
				// In order to do an SDS migration, while still running in production
				// with the cyrus archiveKind, we need to ensure we'll not try to push new
				// object to the cyrus archive.
				// So we check that archive days is set "higher" than the expected runtime
				// needed to push all cyrus archives to SDS.
				ctx.error(
						"WARNING: archiveDays is less than 30 days, use --force if you really want to force sds migration");
				ret &= false;
			}
			if (archiveSizeThreshold != 0) {
				ctx.error(
						"WARNING: archiveSizeTreshold should be 0 to avoid objects being pushed to the archive partition while we are uploading. Use --force to override");
				ret &= false;
			}
		}
		return ret;
	}

	private void migrate(CyrusPartition partition) throws IOException {
		ArrayBlockingQueue<Path> q = new ArrayBlockingQueue<>(workers);
		ExecutorService pool = Executors.newFixedThreadPool(workers);

		Files.walk(partition.archiveParent(), FileVisitOption.FOLLOW_LINKS).filter(p -> {
			File asFile = p.toFile();
			return asFile.isFile() && asFile.getName().endsWith(".");
		}).forEach(p -> {
			long inode;
			try {
				inode = (long) Files.getAttribute(p, "unix:ino");
			} catch (IOException e) {
				throw new ServerFault(e);
			}
			if (cache && migrationMap.getOrDefault(inode, 404) == 200) {
				return;
			}

			try {
				q.put(p); // block until a slot is free
			} catch (InterruptedException ie) {
			}
			pool.submit(() -> {
				CompletableFuture<Response> respFut = pushToSdsProxy(ahc, p);
				try {
					Response resp = respFut.get(30, TimeUnit.SECONDS);
					migrationMap.put(inode, resp.getStatusCode());
					if (uploadCount.incrementAndGet() % 1000 == 0) {
						db.commit();
					}
				} catch (Exception e) {
					throw new ServerFault(e);
				} finally {
					q.remove(); // NOSONAR: We don't care what path we remove
				}
			});
		});
		db.commit();
		pool.shutdown();
		try {
			pool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
		}
	}

	private CompletableFuture<Response> pushToSdsProxy(AsyncHttpClient ahc, Path p) {
		try {
			String fn = p.toFile().getAbsolutePath();
			@SuppressWarnings("deprecation")
			String guid = com.google.common.io.Files.asByteSource(p.toFile()).hash(Hashing.sha1()).toString();
			JsonObject upload = new JsonObject().put("mailbox", "migration").put("guid", guid).put("filename", fn);

			return ahc.preparePut(SDS_ENDPOINT_PUTOBJECT).setBody(upload.encode().getBytes())
					.setHeader("Content-Type", "application/json").execute().toCompletableFuture()
					.thenApply(ahcresp -> {
						ctx.info("{} -> {}: {}", fn, guid, ahcresp.getStatusCode() == 200 ? "OK" : "FAILED");
						return ahcresp;
					});
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private void updateSdsConfiguration(AsyncHttpClient ahc, Map<String, String> sdsConf) {
		JsonObject json = new JsonObject()//
				.put("storeType", sdsConf.get(SysConfKeys.archive_kind.name()))//
				.put("endpoint", sdsConf.get(SysConfKeys.sds_s3_endpoint.name()))//
				.put("accessKey", sdsConf.get(SysConfKeys.sds_s3_access_key.name()))//
				.put("secretKey", sdsConf.get(SysConfKeys.sds_s3_secret_key.name()))//
				.put("region", sdsConf.getOrDefault(SysConfKeys.sds_s3_region.name(), ""))//
				.put("bucket", sdsConf.getOrDefault(SysConfKeys.sds_s3_bucket.name(), ""));
		ListenableFuture<Response> resp = ahc.preparePost(SDS_ENDPOINT_CONFIGURATION).setBody(json.encode().getBytes())
				.setHeader("Content-Type", "application/json").execute();
		try {
			Response response = resp.get(30, TimeUnit.SECONDS);
			if (response.getStatusCode() != 200) {
				String errorMessage = "SDS reconfiguration failed (http error_code:" + response.getStatusCode() + "): "
						+ response.getStatusText();
				ctx.error(errorMessage);
				throw new ServerFault(errorMessage);
			}
		} catch (Exception e) {
			throw new ServerFault(e);
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
