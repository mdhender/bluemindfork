package net.bluemind.dataprotect.mailbox.internal;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

public class BoxFsFolders {
	public final String restoreFolderName;
	public final String restoreDataRoot;
	public final String restoreMetaRoot;
	public final String restoreArchiveRoot;

	public final Set<String> dataPath;
	public final Set<String> metaPath;
	public final Set<String> archivePath;

	private BoxFsFolders(String restoreDataRoot, String restoreMetaRoot, String restoreArchiveRoot,
			String restoreFolderName, Set<String> dataPath, Set<String> metaPath, Set<String> archivePath) {
		this.restoreFolderName = restoreFolderName;

		this.restoreDataRoot = restoreDataRoot + "/" + restoreFolderName;
		this.restoreMetaRoot = restoreMetaRoot + "/" + restoreFolderName;
		this.restoreArchiveRoot = restoreArchiveRoot + "/" + restoreFolderName;

		this.dataPath = dataPath;
		this.metaPath = metaPath;
		this.archivePath = archivePath;
	}

	public Set<String> allFolders() {
		return Stream.of(dataPath, metaPath, archivePath).flatMap(Set::stream).collect(Collectors.toSet());
	}

	public static BoxFsFolders build(ItemValue<Domain> d, ItemValue<Mailbox> mbox, DataProtectGeneration dpg) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String restoreFolderName = "restored-" + sdf.format(dpg.protectionTime);
		String restoreFolderDataRoot = mboxContainer(mbox, d.value, 'r') + fsLogin(mbox.value.name);
		String restoreFolderMetaRoot = mboxMetaContainer(mbox, d.value, 'r') + fsLogin(mbox.value.name);
		String restoreArchiveRoot = mboxArchiveContainer(mbox, d.value, 'r') + fsLogin(mbox.value.name);

		Set<String> dataPath = new HashSet<>();
		Set<String> metaPath = new HashSet<>();
		Set<String> archivePath = new HashSet<>();

		List<Character> letters = new LinkedList<>();
		if (mbox.value.type == Type.user) {
			char letter = mbox.value.name.charAt(0);
			if (!Character.isLetter(letter)) {
				letter = 'q';
			}

			letters.add(letter);

			restoreFolderDataRoot = mboxContainer(mbox, d.value, letter) + fsLogin(mbox.value.name);
			restoreFolderMetaRoot = mboxMetaContainer(mbox, d.value, letter) + fsLogin(mbox.value.name);
			restoreArchiveRoot = mboxArchiveContainer(mbox, d.value, letter) + fsLogin(mbox.value.name);
		} else if (mbox.value.type == Type.mailshare) {
			char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
			for (char c : chars) {
				letters.add(c);
			}
		}

		for (char c : letters) {
			dataPath.add(mboxContainer(mbox, d.value, c) + fsLogin(mbox.value.name));
			metaPath.add(mboxMetaContainer(mbox, d.value, c) + fsLogin(mbox.value.name));
			archivePath.add(mboxArchiveContainer(mbox, d.value, c) + fsLogin(mbox.value.name));
		}

		return new BoxFsFolders(restoreFolderDataRoot, restoreFolderMetaRoot, restoreArchiveRoot, restoreFolderName,
				dataPath, metaPath, archivePath);
	}

	public static String namespace(ItemValue<Mailbox> mbox) {
		return mbox.value.type.cyrAdmPrefix;
	}

	private static String mboxContainer(ItemValue<Mailbox> mbox, Domain d, char oneLetterSplit) {
		return boxContainer("/var/spool/cyrus/data", mbox, d, oneLetterSplit);
	}

	private static String mboxMetaContainer(ItemValue<Mailbox> mbox, Domain d, char oneLetterSplit) {
		return boxContainer("/var/spool/cyrus/meta", mbox, d, oneLetterSplit);
	}

	private static String mboxArchiveContainer(ItemValue<Mailbox> mbox, Domain d, char oneLetterSplit) {
		return boxContainer("/var/spool/bm-hsm/cyrus-archives", mbox, d, oneLetterSplit);
	}

	private static String boxContainer(String root, ItemValue<Mailbox> mbox, Domain d, char oneLetterSplit) {
		StringBuilder cmd = new StringBuilder();
		String dn = d.name;
		String part = CyrusPartition.forServerAndDomain(mbox.value.dataLocation, dn).name;
		cmd.append(root).append("/").append(part);
		char domainLetter = dn.charAt(0);
		if (!Character.isLetter(domainLetter)) {
			domainLetter = 'q';
		}
		cmd.append("/domain/").append(domainLetter);
		cmd.append("/");
		cmd.append(dn);
		cmd.append("/");
		cmd.append(oneLetterSplit);
		cmd.append("/");
		cmd.append(namespace(mbox));
		return cmd.toString();
	}

	public static String fsLogin(String boxName) {
		return boxName.replace('.', '^');
	}
}
