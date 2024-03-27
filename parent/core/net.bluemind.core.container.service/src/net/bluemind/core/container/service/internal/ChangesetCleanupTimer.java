package net.bluemind.core.container.service.internal;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.AbstractVerticle;
import net.bluemind.core.container.service.IChangesetCleanup;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class ChangesetCleanupTimer extends AbstractVerticle {
	ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	private static final int DEFAULT_EXPIRATION = 60;
	private static final Logger logger = LoggerFactory.getLogger(ChangesetCleanupTimer.class);
	private static final Executor executor = Executors
			.newSingleThreadExecutor(new DefaultThreadFactory("changeset-cleanup"));

	@Override
	public void start() {
		VertxPlatform.executeBlockingPeriodic(TimeUnit.DAYS.toMillis(2), this::execute);
	}

	private void execute(Long timerId) {

		executor.execute(() -> {
			int expiration = getExpiration();
			logger.info("Clean deleted changeset elements older than {} days (tid {})", expiration, timerId);
			Set<String> servers = getServers();
			servers.forEach(server -> {
				long time = System.currentTimeMillis();
				IChangesetCleanup service = provider.instance(IChangesetCleanup.class, server);
				service.deleteOldDeletedChangesetItems(expiration);
				time = System.currentTimeMillis() - time;
				logger.info("cleanup process took {}ms.", time);

			});
		});
	}

	private Set<String> getServers() {
		return Topology.get().nodes().stream()
				.filter(iv -> iv.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag())).map(iv -> iv.uid)
				.collect(Collectors.toSet());
	}

	private int getExpiration() {
		ISystemConfiguration confService = provider.instance(ISystemConfiguration.class);
		Integer exp = confService.getValues().integerValue(SysConfKeys.changeset_cleanup_retention_time.name());
		return exp == null ? DEFAULT_EXPIRATION : exp;
	}
}
