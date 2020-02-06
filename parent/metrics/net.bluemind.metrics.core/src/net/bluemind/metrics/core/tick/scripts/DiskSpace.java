package net.bluemind.metrics.core.tick.scripts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.ITickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class DiskSpace implements ITickTemplateProvider {
	private static final Logger logger = LoggerFactory.getLogger(ITickTemplateProvider.class);

	@Override
	public String templateId() {
		return "disk-space";
	}

	@Override
	public InputStream content() {
		return DiskSpace.class.getClassLoader().getResourceAsStream("tickconfig/disk-space.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));
		srvProducts.add(Product.NODE);

		IServer serverApi = ctx.provider().instance(IServer.class, InstallationId.getIdentifier());

		List<TemplateDefinition> defs = new ArrayList<TemplateDefinition>();

		for (Product prod : srvProducts) {
			for (String mountpoint : prod.mountpoints) {
				logger.info("Getting device for mountpoint {}", mountpoint);
				String device = getDeviceFromMountpoint(mountpoint, serverApi, server);
				if (device == null) {
					logger.warn("Couldn't get device for {}", mountpoint);
					continue;
				}
				String alertId = TickTemplateHelper.newId(prod, "disk-space." + mountpoint.replace('/', '-'), server);
				TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
						.withEndPoint(endPointUrl).withProduct(prod).build();
				def.variables.put("device", new JsonObject().put("type", "string").put("value", device));
				logger.info("Definition is {}", def.variables.encodePrettily());
				defs.add(def);
			}
		}
		return defs;
	}

	private String getDeviceFromMountpoint(String mountpoint, IServer serverApi, ItemValue<Server> serv) {
		CommandStatus status = serverApi.submitAndWait(serv.uid, "df -h " + mountpoint + " --output=source");
		String device = null;

		if (status.successful) {
			device = status.output.iterator().next();
			device = device.substring(device.lastIndexOf('\n') + 1);
			if (device.startsWith("/")) {
				device = device.substring(device.lastIndexOf('/') + 1);
			}
		}
		return device;
	}
}
