package net.bluemind.backend.systemconf.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;

public class SystemConfServerHook extends DefaultServerHook {
	private static final Logger logger = LoggerFactory.getLogger(SystemConfServerHook.class);

	private static final String PARAMETER_MYNETWORKS = "mynetworks";

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> item) throws ServerFault {
		ISystemConfiguration systemConfService = ServerSideServiceProvider.getProvider(context.su())
				.instance(ISystemConfiguration.class);
		systemConfService.updateMutableValues(systemConfService.getValues().values);
	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> server) throws ServerFault {
		ISystemConfiguration systemConfService = ServerSideServiceProvider.getProvider(context.su())
				.instance(ISystemConfiguration.class);

		SystemConf systemConf = systemConfService.getValues();
		if (!systemConf.values.containsKey(PARAMETER_MYNETWORKS)) {
			return;
		}

		Set<String> myNetworks = Arrays.asList(systemConf.values.get(PARAMETER_MYNETWORKS).split(",")).stream()
				.filter(net -> net != null
						&& !net.trim().equals(server.value.ip != null ? server.value.ip + "/32" : server.value.fqdn))
				.collect(Collectors.toSet());

		Map<String, String> updatedConf = new HashMap<>();
		updatedConf.put(PARAMETER_MYNETWORKS, String.join(",", myNetworks));

		logger.info(String.join(",", myNetworks));
		systemConfService.updateMutableValues(updatedConf);
	}
}
