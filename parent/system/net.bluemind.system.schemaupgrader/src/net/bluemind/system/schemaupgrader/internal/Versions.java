package net.bluemind.system.schemaupgrader.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.system.schemaupgrader.Updater;

public class Versions {

	public Versions(VersionInfo from, VersionInfo to) {
		super();
		this.from = from;
		this.to = to;
	}

	private VersionInfo from;
	private VersionInfo to;

	public VersionInfo getFrom() {
		return from;
	}

	public void setFrom(VersionInfo from) {
		this.from = from;
	}

	public VersionInfo getTo() {
		return to;
	}

	public void setTo(VersionInfo to) {
		this.to = to;
	}

	public static Comparator<Updater> getComparator() {
		return new Comparator<Updater>() {
			@Override
			public int compare(Updater updater1, Updater updater2) {
				int relMajor1 = updater1.major();
				int relMajor2 = updater2.major();
				if (relMajor1 != relMajor2) {
					return relMajor1 - relMajor2;
				} else {
					int rel1 = updater1.build();
					int rel2 = updater2.build();
					// sort by release number
					if (rel1 != rel2) {
						return rel1 - rel2;
					} else {
						String version1 = relMajor1 + "." + rel1;
						String version2 = relMajor2 + "." + rel2;
						return version1.compareTo(version2);
					}
				}
			}
		};
	}

	public static void sort(List<Updater> updaters) {
		Collections.sort(updaters, getComparator());
	}

}
