package net.bluemind.system.service.hooks;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.api.Database;
import net.bluemind.system.persistence.Upgrader;
import net.bluemind.system.persistence.Upgrader.UpgradePhase;
import net.bluemind.system.persistence.UpgraderStore;
import net.bluemind.system.schemaupgrader.runner.SchemaUpgrade;
import net.bluemind.system.service.helper.UpgraderList;

public class UpgraderServerHook extends DefaultServerHook {

	private static final Logger logger = LoggerFactory.getLogger(UpgraderServerHook.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!(tag.equals("bm/pgsql") || tag.equals("bm/pgsql-data"))) {
			return;
		}

		Set<String> upgraders = UpgraderList.get();
		try {
			List<String> schemaUpgrades = SchemaUpgrade.getUpgradePath().stream()
					.map(u -> Upgrader.toId(u.date(), u.sequence())).toList();
			upgraders.addAll(schemaUpgrades);
		} catch (Exception e) {
			// installation might have no subscription
		}

		UpgraderStore store = new UpgraderStore(context.getDataSource());
		try {
			store.needsMigration();
		} catch (SQLException e) {
			throw new ServerFault("Cannot create upgrader table", e);
		}
		for (String updater : upgraders) {
			registerUpgrader(server.uid, store, updater);
			if (tag.equals("bm/pgsql")) {
				registerUpgrader("master", store, updater);
			}
		}
	}

	private void registerUpgrader(String serverUid, UpgraderStore store, String updater) {
		Upgrader upgrader = new Upgrader();
		upgrader.phase = UpgradePhase.SCHEMA_UPGRADE;
		upgrader.server = serverUid;
		upgrader.success = true;
		upgrader.upgraderId = updater;
		try {
			for (Database db : Database.values()) {
				upgrader.database = db;
				if (!store.upgraderRegistered(upgrader.upgraderId, serverUid, db)) {
					store.store(upgrader);
				}
			}
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

}
