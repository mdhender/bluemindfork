package net.bluemind.backend.cyrus.mailboxesdb;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class MailboxesDbEntry {
	private static final Logger logger = LoggerFactory.getLogger(MailboxesDbEntry.class);

	private static final String TAB = "\t";
	public static final String ACL_24LINE_CVT3_SEP = new String(new byte[] { (byte) 0xFF, (byte) 0x89 },
			StandardCharsets.UTF_8);
	private static final String SPACE = " ";

	// domain.tld!user.admin\t%(A %(aclsuid acl) P partition M 1533736213)
	private static final Pattern CYRUS30_LINE = Pattern.compile("^[^\t]+\t%\\([^\t]+\\)$");
	// domain.tld!user.admin\t0 partition acluid\tacl\t
	private static final Pattern CYRUS24_LINE = Pattern.compile("^[^\t]+\t[0-9] [^ \t]+( [^ ]+){0,1}$");

	public static class Acl {
		public final String name;
		public String perms;

		public Acl(String name, String perms) {
			this.name = name;
			this.perms = perms;
		}

		@Override
		public String toString() {
			if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(perms)) {
				return null;
			}

			return String.format("%s %s", name, perms);
		}
	}

	public final String name;
	public final Optional<String> uid;
	public String partition;
	public long timestamp;
	public List<Acl> acls = new LinkedList<>();

	public static MailboxesDbEntry getFromString(String mailboxesDbEntryAsString) {
		if (mailboxesDbEntryAsString.trim().isEmpty()) {
			return null;
		}

		Optional<MailboxesDbEntry> mailboxDbEntry;
		if (CYRUS30_LINE.matcher(mailboxesDbEntryAsString).matches()) {
			logger.info("Read cyrus 3.0 line format: {}", mailboxesDbEntryAsString);
			mailboxDbEntry = cyrus3Line(mailboxesDbEntryAsString);
		} else {
			mailboxesDbEntryAsString = mailboxesDbEntryAsString.replace(ACL_24LINE_CVT3_SEP, TAB);
			if (CYRUS24_LINE.matcher(mailboxesDbEntryAsString).matches()) {
				logger.info("Read cyrus 2.4 line format: {}", mailboxesDbEntryAsString);
				mailboxDbEntry = cyrus24Line(mailboxesDbEntryAsString);
			} else {
				logger.error("Invalid mailboxes line format: {}", mailboxesDbEntryAsString);
				return null;
			}
		}

		if (mailboxDbEntry != null && mailboxDbEntry.isPresent()) {
			return mailboxDbEntry.get();
		}

		return null;
	}

	private static Optional<MailboxesDbEntry> cyrus24Line(String line) {
		String name = line.substring(0, line.indexOf(TAB));
		String partition = getCyrusField(line.substring(line.indexOf(TAB)), SPACE);

		List<Acl> acls = getCyrus24Acls(line.substring(line.indexOf(TAB)));

		return Optional.of(new MailboxesDbEntry(name, partition, acls));
	}

	private static List<Acl> getCyrus24Acls(String line) {
		int start = line.indexOf(SPACE, line.indexOf(SPACE) + 1);
		if (start == -1) {
			logger.info("No ACLs found in {}", line);
			return Collections.emptyList();
		}

		String aclsAsString = line.substring(start + 1);
		if (aclsAsString.endsWith(TAB)) {
			aclsAsString = aclsAsString.substring(0, aclsAsString.length() - 1);
		}

		String[] aclsSplitted = aclsAsString.split(TAB);
		if ((aclsSplitted.length % 2) != 0) {
			logger.info("Invalid ACLs list {}", aclsAsString);
			return Collections.emptyList();
		}

		List<Acl> acls = new LinkedList<>();
		for (int i = 0; i < aclsSplitted.length; i += 2) {
			acls.add(new Acl(aclsSplitted[i], aclsSplitted[i + 1]));
		}

		return acls;
	}

	private static Optional<MailboxesDbEntry> cyrus3Line(String line) {
		String[] lineParts = line.split(TAB);
		if (lineParts.length != 2) {
			logger.error("Ignoring invalid line {}", line);
			return Optional.empty();
		}

		String name = lineParts[0];
		String balInfos = lineParts[1];

		String uid = getCyrusField(balInfos, "I ");
		String partition = getCyrusField(balInfos, "P ");

		List<MailboxesDbEntry.Acl> acls = getCyrus3Acls(balInfos);

		long timestamp = 0;
		try {
			timestamp = Long.parseLong(getCyrusField(balInfos, "M "));
		} catch (NumberFormatException nfe) {
			timestamp = System.currentTimeMillis() / 1000;
		}

		return Optional.of(new MailboxesDbEntry(name, uid, partition, acls, timestamp));
	}

	private static String getCyrusField(String balInfos, String startMark) {
		int start = balInfos.indexOf(startMark);
		if (start == -1) {
			logger.info("No value found in {} starting at '{}'", balInfos, startMark);
			return null;
		}
		start += startMark.length();

		int end = balInfos.indexOf(SPACE, start);
		if (end == -1) {
			end = balInfos.indexOf(")", start);

			if (end == -1) {
				end = balInfos.length();
			}
		}

		return balInfos.substring(start, end);
	}

	private static List<Acl> getCyrus3Acls(String balInfos) {
		String aclStart = "A %(";
		int start = balInfos.indexOf(aclStart);
		if (start == -1) {
			logger.info("No ACLs found in {}", balInfos);
			return Collections.emptyList();
		}
		start += aclStart.length();

		int end = balInfos.indexOf(")", start);
		String aclsAsString = balInfos.substring(start, end);
		String[] aclsSplitted = aclsAsString.split(SPACE);
		if ((aclsSplitted.length % 2) != 0) {
			logger.info("Invalid ACLs list {}", aclsAsString);
			return Collections.emptyList();
		}

		List<Acl> acls = new LinkedList<>();
		for (int i = 0; i < aclsSplitted.length; i += 2) {
			acls.add(new Acl(aclsSplitted[i], aclsSplitted[i + 1]));
		}

		return acls;
	}

	public MailboxesDbEntry(String name, String partition, List<Acl> acls) {
		this.name = name;
		this.uid = Optional.empty();
		this.partition = partition;
		this.timestamp = System.currentTimeMillis() / 1000;
		this.acls = acls;
	}

	public MailboxesDbEntry(String name, String uid, String partition, List<Acl> acls, long timestamp) {
		this.name = name;

		if (!Strings.isNullOrEmpty(uid)) {
			this.uid = Optional.of(uid);
		} else {
			this.uid = Optional.empty();
		}

		this.partition = partition;
		this.acls = acls;
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		StringBuilder mailboxInfos = new StringBuilder();

		if (!acls.isEmpty()) {
			List<String> aclsList = acls.stream().map(a -> a.toString()).filter(Objects::nonNull)
					.collect(Collectors.toList());
			if (!aclsList.isEmpty()) {
				mailboxInfos.append("A %(").append(String.join(" ", aclsList)).append(") ");
			}
		}

		if (uid.isPresent()) {
			mailboxInfos.append("I ").append(uid.get()).append(" ");
		}

		if (!Strings.isNullOrEmpty(partition)) {
			mailboxInfos.append("P ").append(partition).append(" ");
		}

		mailboxInfos.append("M ").append(timestamp).append(" ");
		if (mailboxInfos.length() == 0) {
			return null;
		}

		return String.format("%s%s%%(%s)", name, TAB, mailboxInfos.toString().trim());
	}
}
