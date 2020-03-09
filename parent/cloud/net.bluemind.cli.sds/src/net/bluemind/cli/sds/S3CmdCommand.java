package net.bluemind.cli.sds;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

@Command(name = "s3cmd", description = "configure s3cmd, or s4cmd")
public class S3CmdCommand implements ICmdLet, Runnable {
	CliContext ctx;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sds");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return S3CmdCommand.class;
		}

	}

	public S3CmdCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Option(name = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	@Override
	public void run() {
		StringBuilder stringbuilder = new StringBuilder().append("[default]\n");
		ISystemConfiguration sysconfapi = ctx.adminApi().instance(ISystemConfiguration.class);
		SystemConf sysconf = sysconfapi.getValues();
		String endpointValue = sysconf.stringValue(SysConfKeys.sds_s3_endpoint.name());
		String endpoint = endpointValue;
		try {
			URI uri = new URI(endpointValue);
			endpoint = uri.getHost() + ":" + uri.getPort();
		} catch (URISyntaxException e) {
			ctx.error(e.getMessage());
			System.exit(1);
		}
		String accesskey = sysconf.stringValue(SysConfKeys.sds_s3_access_key.name());
		String secretkey = sysconf.stringValue(SysConfKeys.sds_s3_secret_key.name());
		String region = sysconf.stringValue(SysConfKeys.sds_s3_region.name());

		String cfg = stringbuilder//
				.append("host_base = ").append(endpoint).append("\n")//
				.append("host_bucket = ").append("%(bucket).").append(endpoint).append("\n")//
				.append("access_key = ").append(accesskey).append("\n")//
				.append("secret_key = ").append(secretkey).append("\n")//
				.append("use_https = ").append(endpointValue.startsWith("https") ? "True" : "False").append("\n")//
				.append("bucket_location = ").append(region).append("\n")//
				.append("signature_v2 = False").append("\n").toString();

		if (dry) {
			ctx.info(cfg);
		} else {
			try {
				String path = System.getProperty("user.home") + "/.s3cfg";
				Files.write(Paths.get(path), cfg.getBytes());
				ctx.info(cfg);
				ctx.info("written to " + path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
