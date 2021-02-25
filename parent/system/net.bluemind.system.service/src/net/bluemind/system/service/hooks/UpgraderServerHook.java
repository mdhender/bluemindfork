package net.bluemind.system.service.hooks;

import java.sql.SQLException;
import java.util.List;

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
import net.bluemind.system.schemaupgrader.DatedUpdater;
import net.bluemind.system.schemaupgrader.runner.SchemaUpgrade;

public class UpgraderServerHook extends DefaultServerHook {

	private static final Logger logger = LoggerFactory.getLogger(UpgraderServerHook.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!(tag.equals("bm/pgsql") || tag.equals("bm/pgsql-data"))) {
			return;
		}

		List<DatedUpdater> upgraders = null;
		try {
			upgraders = SchemaUpgrade.getUpgradePath();
		} catch (ServerFault e) {
			logger.info("Upgraders not available. Skipping upgrader registration");
			return;
		}

		UpgraderStore store = new UpgraderStore(context.getDataSource());
		try {
			store.needsMigration();
		} catch (SQLException e) {
			throw new ServerFault("Cannot create upgrader table", e);
		}
		for (DatedUpdater updater : upgraders) {
			registerUpgrader(server.uid, store, updater);
			if (tag.equals("bm/pgsql")) {
				registerUpgrader("master", store, updater);
			}
		}
	}

	private void registerUpgrader(String serverUid, UpgraderStore store, DatedUpdater updater) {
		Upgrader upgrader = new Upgrader();
		upgrader.phase = UpgradePhase.SCHEMA_UPGRADE;
		upgrader.server = serverUid;
		upgrader.success = true;
		upgrader.upgraderId = Upgrader.toId(updater.date(), updater.sequence());
		try {
			for (Database db : Database.values()) {
				upgrader.database = db;
				if (!store.upgraderRegistered(upgrader.upgraderId, serverUid, updater.database())) {
					store.store(upgrader);
				}
			}
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

}
