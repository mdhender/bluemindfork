package net.bluemind.backend.cyrus.mailboxesdb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;

/**
 * Use this carefully. Must be used on upgrade only and may take long time!
 */
public class MailboxesDb {
	private static final Logger logger = LoggerFactory.getLogger(MailboxesDb.class);

	private static final String LF = "\n";

	private static final String cvt_cyrusdb = "/usr/sbin/cvt_cyrusdb";
	private static final String mailBoxesDb = "/var/lib/cyrus/mailboxes.db";
	private static final String TYPE_TWOSKIP = "twoskip";
	private static final String TYPE_FLAT = "flat";
	private static final Path tmpFile = Paths.get("/tmp/mailboxes.flat");
	private static String cmdTwoSkipToFlat = String.format("%s %s %s %s %s", cvt_cyrusdb, mailBoxesDb, TYPE_TWOSKIP,
			tmpFile, TYPE_FLAT);
	private static String cmdFlatToTwoSkip = String.format("%s %s %s %s %s", cvt_cyrusdb,
			tmpFile.toFile().getAbsolutePath(), TYPE_FLAT, mailBoxesDb, TYPE_TWOSKIP);

	private final String serverIp;
	public final List<MailboxesDbEntry> mailboxesDbEntry;

	/**
	 * Stop cyrus on target server until mailboxesDb write or restart by another way
	 * 
	 * @param nc
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static MailboxesDb loadFromMailboxesDb(String serverIp) throws FileNotFoundException, IOException {
		INodeClient nodeClient = NodeActivator.get(serverIp);

		logger.info("Stopping cyrus on {}", serverIp);
		exec(nodeClient, "service bm-cyrus-imapd stop");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}

		logger.info("Converting {} skiplist to {} plain text on {}", mailBoxesDb, tmpFile, serverIp);
		exec(nodeClient, "rm -f " + tmpFile);
		exec(nodeClient, cmdTwoSkipToFlat);

		logger.info("Reading from {} on {}", tmpFile, serverIp);
		byte[] content = nodeClient.read(tmpFile.toFile().getAbsolutePath());
		String[] lines = new String(content, StandardCharsets.UTF_8).split(LF);

		return new MailboxesDb(serverIp, lines);
	}

	public static MailboxesDb loadFromLineArray(String[] lines) {
		return new MailboxesDb(null, lines);
	}

	public static void writeToMailboxesDb(MailboxesDb mailboxesDb) throws IOException {
		INodeClient nodeClient = NodeActivator.get(mailboxesDb.serverIp);

		logger.info("Stoping cyrus on {}", mailboxesDb.serverIp);
		exec(nodeClient, "service bm-cyrus-imapd stop");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}

		logger.info("Writing {} as plain text on {}", tmpFile, mailboxesDb.serverIp);
		exec(nodeClient, "rm -f " + tmpFile);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mailboxesDb.getFlatMailboxesDb(out);
		nodeClient.writeFile(tmpFile.toString(), new ByteArrayInputStream(out.toByteArray()));
		exec(nodeClient, "chown cyrus:mail " + tmpFile);

		String mailBoxesDbBackup = mailBoxesDb + "-" + System.currentTimeMillis();
		logger.info("Backuping {} to {}", mailBoxesDb, mailBoxesDbBackup);
		exec(nodeClient, "cp " + mailBoxesDb + " " + mailBoxesDbBackup);
		exec(nodeClient, "chown cyrus:mail " + mailBoxesDb);

		logger.info("Converting {} plain text to {} twoskip text on {}", tmpFile, mailBoxesDb, mailboxesDb.serverIp);
		exec(nodeClient, cmdFlatToTwoSkip);
		exec(nodeClient, "chown cyrus:mail " + mailBoxesDb);
		exec(nodeClient, "chmod 600 " + mailBoxesDb);

		logger.info("Starting cyrus on {}", mailboxesDb.serverIp);
		exec(nodeClient, "service bm-cyrus-imapd start");

		new NetworkHelper(mailboxesDb.serverIp).waitForListeningPort(1143, 30, TimeUnit.SECONDS);
	}

	private static void exec(INodeClient nodeClient, String cmd) {
		ExitList exec = NCUtils.exec(nodeClient, cmd);
		logger.info("Cmd {}, exit code: {}", cmd, exec.getExitCode());
		for (String str : exec) {
			logger.info("Messages : {}", str);
		}
	}

	private MailboxesDb(String serverIp, String[] lines) {
		this.serverIp = serverIp;

		mailboxesDbEntry = Arrays.stream(lines).map(line -> MailboxesDbEntry.getFromString(line))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	public void getFlatMailboxesDb(OutputStream os) throws IOException {
		os.write((String.join(LF, getMailboxesDbLinesList()) + LF).getBytes());
	}

	public String getFlatMailboxesDb() throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		getFlatMailboxesDb(os);
		return os.toString();
	}

	private List<String> getMailboxesDbLinesList() {
		return mailboxesDbEntry.stream().map(mde -> mde.toString()).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
