package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology.PromotingServer;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.server.api.IServer;

public class RestoreDomains {

	private static final Logger logger = LoggerFactory.getLogger(RestoreDomains.class);

	private final String installationId;
	private final IServiceProvider target;
	private final Collection<PromotingServer> servers;

	public RestoreDomains(String installationId, IServiceProvider target, Collection<PromotingServer> servers) {
		this.installationId = installationId;
		this.target = target;
		this.servers = servers;
	}

	public Map<String, ItemValue<Domain>> restore(IServerTaskMonitor monitor, List<DataElement> domains) {
		IServer topoApi = target.instance(IServer.class, installationId);
		ValueReader<ItemValue<Domain>> domReader = JsonUtils.reader(new TypeReference<ItemValue<Domain>>() {
		});
		ValueReader<ItemValue<DomainSettings>> setReader = JsonUtils
				.reader(new TypeReference<ItemValue<DomainSettings>>() {
				});
		IDomains domApi = target.instance(IDomains.class);

		Map<String, ItemValue<Domain>> domainsToHandle = new HashMap<>();
		domains.forEach(domDE -> {
			String domJs = new String(domDE.payload);
			switch (domDE.key.valueClass) {
			case "net.bluemind.domain.api.Domain":
				ItemValue<Domain> dom = domReader.read(domJs);
				if (dom.uid.equals("global.virt")) {
					return;
				}
				ItemValue<Domain> known = domApi.get(dom.uid);
				if (known != null) {
					logger.info("UPDATE DOMAIN {}", dom);
					domApi.update(dom.uid, dom.value);
				} else {
					logger.info("CREATE DOMAIN {}", dom);
					domApi.create(dom.uid, dom.value);
					for (PromotingServer iv : servers) {
						for (String tag : iv.clone.value.tags) {
							topoApi.assign(iv.clone.uid, dom.uid, tag);
						}
						monitor.log("assign " + iv.clone.uid + " to " + dom.uid);
					}
				}
				domainsToHandle.put(dom.uid, dom);
				break;
			case "net.bluemind.domain.api.DomainSettings":
				ItemValue<DomainSettings> set = setReader.read(domJs);
				IDomainSettings setApi = target.instance(IDomainSettings.class, set.uid);
				setApi.set(set.value.settings);
				logger.info("Set settings of {}", set.uid);
				break;
			default:
				System.err.println("Unhandled " + domDE.key.valueClass);
				break;
			}
		});
		monitor.progress(1, "Dealt with " + domainsToHandle.size() + " domain(s)");
		return domainsToHandle;
	}

}
