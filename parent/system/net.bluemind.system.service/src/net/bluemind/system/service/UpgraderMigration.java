/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.system.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.Database;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.persistence.Upgrader;
import net.bluemind.system.persistence.Upgrader.UpgradePhase;
import net.bluemind.system.persistence.UpgraderStore;
import net.bluemind.system.schemaupgrader.runner.SchemaUpgrade;

public class UpgraderMigration {

	// 4.1.48913 4.2.4
	// 4.1.48893 4.2.1 --> < 440
	// 4.1.48891 4.2.0 --> < 440
	// 4.1.47249 4.1.5 --> < 410
	// 4.1.47247 4.1.4 --> < 410
	// 4.1.47235 4.1.3 --> < 410
	// 4.1.47231 4.1.2 --> < 410
	// 4.1.47229 4.1.1 --> < 400
	// 4.1.47224 4.1 --> < 400
	// 4.1.43927 4.0.15 --> < 300
	// 4.1.43922 4.0.14 --> < 300
	// 4.1.43921 4.0.13 --> < 300
	// 4.1.43914 4.0.12 --> < 300
	// 4.1.43904 4.0.11 --> < 300
	// 4.1.43876 4.0.10 --> < 290
	// 4.1.43871 4.0.9 --> < 290
	// 4.1.43860 4.0.8 --> < 280
	// 4.1.43842 4.0.7 --> < 250
	// 4.1.43802 4.0.6 --> < 250
	// 4.1.43707 4.0.5 --> < 220
	// 4.1.42538 4.0.4 --> < 190
	// 4.1.42345 4.0.3 --> < 190
	// 4.1.42304 4.0.2 --> < 190
	// 4.1.42252 4.0.1 --> < 190
	// 4.1.42217 4.0 --> < 190

	public static void migrate(UpgraderStore store, VersionInfo from, List<String> servers) throws Exception {

		if (from.major.equals("3")) {
			return;
		}

		Set<Integer> sequences = new HashSet<>();

		String installationDateAsString = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues()
				.stringValue(SysConfKeys.installation_release_date.name());
		if (!Strings.isNullOrEmpty(installationDateAsString)) {
			// installation >= 4.3, we register all upgraders 4.0 -> 4.3 + all upgraders <
			// installation date

			sequences.addAll(Arrays.asList(430, 420, 410, 400, 390, 380, 370, 360, 350, 340, 330, 320, 310, 300, 290,
					280, 270, 260, 250, 240, 230, 220, 210, 200, 190, 180, 170, 160, 150, 140, 130, 120, 110, 100, 90,
					80, 70, 60, 50, 40, 50, 60, 50, 40, 30, 20, 10, 7, 0));

			registerUpgradersByInstallationDate(store, servers, installationDateAsString);
		} else {

			int fromRelease = Integer.parseInt(from.release);

			sequences.addAll(Arrays.asList(180, 170, 160, 150, 140, 130, 120, 110, 100, 90, 80, 70, 60, 50, 40, 50, 60,
					50, 40, 30, 20, 10, 7, 0));

			if (fromRelease > 42538) {
				sequences.addAll(Arrays.asList(210, 200, 190));
			}

			if (fromRelease > 43707) {
				sequences.addAll(Arrays.asList(240, 230, 220));
			}

			if (fromRelease > 43842) {
				sequences.addAll(Arrays.asList(270, 260, 250));
			}

			if (fromRelease > 43860) {
				sequences.addAll(Arrays.asList(280));
			}

			if (fromRelease > 43876) {
				sequences.addAll(Arrays.asList(290));
			}

			if (fromRelease > 43927) {
				sequences.addAll(Arrays.asList(300, 310, 320, 330, 340, 350, 360, 370, 380, 390));
			}

			if (fromRelease > 47229) {
				sequences.addAll(Arrays.asList(400));
			}

			if (fromRelease > 47249) {
				sequences.addAll(Arrays.asList(430, 420, 410));
			}
		}

		for (int seq : sequences) {
			registerUpgrader(store, servers, Upgrader.toId(java.sql.Date.valueOf(LocalDate.of(2020, 4, 28)), seq));
		}

	}

	private static void registerUpgrader(UpgraderStore store, List<String> servers, String id) {
		for (String server : servers) {
			for (Database db : Database.values()) {
				Upgrader upgrader = new Upgrader();
				upgrader.database = db;
				upgrader.phase = UpgradePhase.SCHEMA_UPGRADE;
				upgrader.server = server;
				upgrader.success = true;
				upgrader.upgraderId = id;
				store.add(upgrader);
			}
		}
	}

	private static void registerUpgradersByInstallationDate(UpgraderStore store, List<String> servers,
			String installationDateAsString) throws Exception {
		Date installationDate = new SimpleDateFormat("yyyy-MM-dd").parse(installationDateAsString);

		SchemaUpgrade.getUpgradePath().forEach(upgrader -> {
			Date upgraderDate = upgrader.date();
			if (upgraderDate.before(installationDate)) {
				registerUpgrader(store, servers, Upgrader.toId(upgrader.date(), upgrader.sequence()));
			}
		});
	}

}
