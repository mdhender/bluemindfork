package net.bluemind.server.node.hook;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class NodeHook extends DefaultServerHook {
	private static final Logger logger = LoggerFactory.getLogger(NodeHook.class);

	public static final String serverCert = "/etc/bm/bm.jks";
	public static final String clientCert = "/etc/bm/nodeclient_keystore.jks";
	public static final String trustClientCert = "/etc/bm/nodeclient_truststore.jks";
	public static final String cacert = "/var/lib/bm-ca/cacert.pem";
	public static final String bmcoretok = "/etc/bm/bm-core.tok";

	// BM-10505
	public static final String bmCerts = "/etc/ssl/certs/bm_cert.pem";
	public static final String dhParam = "/etc/nginx/bm_dhparam.pem";

	public NodeHook() {
	}

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> server) throws ServerFault {
		logger.info("***** new node, copy " + trustClientCert + " to trigger clientCert auth");

		String adr = server.value.address();
		// setup keys & secure node communication
		try {
			if (!new File(clientCert).exists()) {
				fullInitLocalhost();
			}
			INodeClient remote = NodeActivator.get(adr);
			remote.writeFile(serverCert, new ByteArrayInputStream(Files.toByteArray(new File(serverCert))));
			remote.executeCommandNoOut("chmod 400 " + serverCert);
			remote.writeFile(trustClientCert, new ByteArrayInputStream(Files.toByteArray(new File(trustClientCert))));
			remote.executeCommandNoOut("chmod 400 " + trustClientCert);
			remote.writeFile(cacert, new ByteArrayInputStream(Files.toByteArray(new File(cacert))));
			// make it easy to figure out which server we are
			remote.writeFile("/etc/bm/server.uid", new ByteArrayInputStream(server.uid.getBytes()));
			remote.ping();
		} catch (Exception sf) {
			logger.info("sf: " + sf.getMessage());
			sleep();
		}

		// copy ini
		try {

			INodeClient remote = NodeActivator.get(adr);
			File f = new File("/etc/bm/bm.ini." + adr);
			if (!f.exists()) {
				f = new File("/etc/bm/bm.ini");
			} else {
				logger.info("Using overriden bm.ini for host {}", adr);
			}
			remote.writeFile("/etc/bm/bm.ini", new ByteArrayInputStream(Files.toByteArray(f)));

			remote.writeFile(bmcoretok, new ByteArrayInputStream(Files.toByteArray(new File(bmcoretok))));
			remote.executeCommandNoOut("chmod 440 " + bmcoretok);
			remote.executeCommandNoOut("chown root:bluemind " + bmcoretok);

			if (!NCUtils.connectedToMyself(remote)) {
				if (!new File("/etc/bm/skip.restart").exists()) {
					NCUtils.execNoOut(remote, "/usr/bin/bmctl restart");
				}
			}
		} catch (Exception sf) {
			logger.info("sf: " + sf.getMessage());
		}

	}

	private void sleep() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}

	private void fullInitLocalhost() throws ServerFault {
		logger.info("Generating on myself...");

		logger.info("Ping node before doing anyhting");
		INodeClient nc = NodeActivator.get("127.0.0.1");
		nc.ping();

		try {
			File theScript = File.createTempFile("client_cert", ".sh");
			try (InputStream in = NodeHook.class.getClassLoader().getResourceAsStream("data/client_cert.sh")) {
				try (OutputStream out = new FileOutputStream(theScript)) {
					ByteStreams.copy(in, out);
				}
			}
			ProcessBuilder pb = new ProcessBuilder().command(new String[] { "/bin/bash", theScript.getAbsolutePath() });
			pb.redirectErrorStream(true);
			Process process = pb.start();
			InputStream in = process.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			do {
				line = br.readLine();
				logger.info(line != null ? line : "---");
			} while (line != null);

			int exit = process.waitFor();
			theScript.delete();
			logger.info("client_cert.sh exited: {}", exit);
			// force server to restart in secure mode
			nc.ping();
		} catch (ServerFault sf) {
			// leave some time to restart as ssl
			sleep();
			logger.info("Got server fault, node has restarted in secure mode.", sf);
			// now my connection should be secure
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {

		if ("bm/core".equals(tag)) {
			newCore(server.value);
		}

		if ("mail/smtp-edge".equals(tag)) {
			transferEdgeConfig(server, server.value.address());
		}

	}

	private void newCore(Server s) {
		if ("127.0.0.1".equals(s.address()) || "localhost".equals(s.address())) {
			return;
		}
		logger.info("***** new core, must copy " + clientCert);
		try {
			INodeClient remote = NodeActivator.get(s.address());
			remote.writeFile(clientCert, new ByteArrayInputStream(Files.toByteArray(new File(clientCert))));
			remote.executeCommandNoOut("chmod 400 " + clientCert);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void transferEdgeConfig(ItemValue<Server> server, String adr) {
		// BM-10505
		try {
			INodeClient remote = NodeActivator.get(adr);
			remote.writeFile(bmCerts, new ByteArrayInputStream(Files.toByteArray(new File(bmCerts))));
			remote.writeFile(dhParam, new ByteArrayInputStream(Files.toByteArray(new File(dhParam))));
			if (!NCUtils.connectedToMyself(remote)) {
				NCUtils.execNoOut(remote, "service bm-nginx reload");
			}
		} catch (Exception sf) {
			throw new ServerFault("Cannot transfer Edge config", sf);
		}
	}

}
