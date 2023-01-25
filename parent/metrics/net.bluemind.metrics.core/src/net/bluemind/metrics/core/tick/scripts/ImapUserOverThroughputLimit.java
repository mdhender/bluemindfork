package net.bluemind.metrics.core.tick.scripts;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.imap.endpoint.EndpointConfig;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.BasicTickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;

public class ImapUserOverThroughputLimit extends BasicTickTemplateProvider {

	@Override
	public String templateId() {
		return "imap-user-over-throughput-limit";
	}

	@Override
	public InputStream content() {
		return MemcachedEvictions.class.getClassLoader()
				.getResourceAsStream("tickconfig/imap-user-over-throughput-limit.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));

		ISystemConfiguration sysConfApi = ctx.provider().instance(ISystemConfiguration.class);
		Integer maxSize = sysConfApi.getValues().integerValue("message_size_limit");
		maxSize = maxSize == null ? 20_000_000 : maxSize;
		long capacity = EndpointConfig.get().getBytes("imap.throughput.capacity");
		Duration duration = EndpointConfig.get().getDuration("imap.throughput.period");

		int maxWaitingTimeInMs = 20_000;

		List<TemplateDefinition> defs = new ArrayList<>();
		if (srvProducts.contains(Product.CORE)) {
			String alertId = TickTemplateHelper.newId(Product.CORE, "imap-usersWaitingDuration", server);
			TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
					.withEndPoint(endPointUrl).withProduct(Product.CORE)
					.withVariable("alertId", "bm-core_imap-usersWaitingDuration-{{ index .Tags \"user\"}}_bm-master")
					.withVariable("maxWaitingTimeInMs", maxWaitingTimeInMs).build();
			defs.add(def);
		}
		return defs;
	}

}