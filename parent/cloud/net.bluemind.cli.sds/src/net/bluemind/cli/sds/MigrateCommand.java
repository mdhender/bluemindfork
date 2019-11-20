package net.bluemind.cli.sds;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.vertx.java.core.json.JsonObject;

import com.google.common.hash.Hashing;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;

@Command(name = "migrate", description = "Migrates messages from cyrus archive partition to object store")
public class MigrateCommand implements ICmdLet, Runnable {
	CliContext ctx;

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

	public MigrateCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Option(name = "--dry", description = "Dry-run (do not write to object store)")
	public boolean dry = false;

	@Arguments(required = true, description = "domain name or alias")
	public String targetDomain;

	@Override
	public void run() {
		File sdsProxyConf = new File("/etc/bm-sds-proxy/config.json");
		if (!sdsProxyConf.exists()) {
			ctx.error(sdsProxyConf.getAbsolutePath() + " does not exist.");
			ctx.info("This command should be executed from an object-store enabled mailbox server.");
			System.exit(1);
		}
		CliUtils cliUtils = new CliUtils(ctx);
		String domain = cliUtils.getDomainUidFromDomain(targetDomain);
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

	private void migrate(CyrusPartition partition) throws IOException {
		AsyncHttpClient ahc = new AsyncHttpClient();
		Files.walk(partition.archiveParent(), FileVisitOption.FOLLOW_LINKS).filter(p -> {
			File asFile = p.toFile();
			return asFile.isFile() && asFile.getName().endsWith(".");
		}).forEach(p -> {
			pushToSdsProxy(ahc, p);
		});
	}

	private void pushToSdsProxy(AsyncHttpClient ahc, Path p) {
		try {
			String fn = p.toFile().getAbsolutePath();
			@SuppressWarnings("deprecation")
			String guid = com.google.common.io.Files.asByteSource(p.toFile()).hash(Hashing.sha1()).toString();
			JsonObject upload = new JsonObject().putString("mailbox", "migration").putString("guid", guid)
					.putString("filename", fn);
			if (!dry) {
				ListenableFuture<Response> resp = ahc.preparePut("http://127.0.0.1:8091/sds")
						.setBody(upload.encode().getBytes()).setHeader("Content-Type", "application/json").execute();
				resp.get(5, TimeUnit.SECONDS);
			}
			ctx.info(fn + " -> " + guid);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

}
