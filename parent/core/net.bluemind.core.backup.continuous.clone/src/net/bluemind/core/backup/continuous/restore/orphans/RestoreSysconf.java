package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.SysconfOverride;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;

public class RestoreSysconf {

	private static final Logger logger = LoggerFactory.getLogger(RestoreSysconf.class);

	private final IServiceProvider target;

	private SysconfOverride confOver;

	public RestoreSysconf(IServiceProvider target, SysconfOverride confOver) {
		this.target = target;
		this.confOver = confOver;
	}

	public SystemConf restore(IServerTaskMonitor monitor, List<DataElement> sysconfs) {
		ISystemConfiguration confApi = target.instance(ISystemConfiguration.class);
		if (sysconfs.isEmpty()) {
			logger.warn("No sysconf, using existing one");
			return confApi.getValues();
		}
		DataElement last = sysconfs.get(sysconfs.size() - 1);
		ValueReader<ItemValue<SystemConf>> scReader = JsonUtils.reader(new TypeReference<ItemValue<SystemConf>>() {
		});
		SystemConf sysconf = scReader.read(new String(last.payload)).value;
		if (sysconf != null) {
			monitor.log("Restore system configuration...");
			sysconf.values.putAll(confOver.getOverrides());
			confApi.updateMutableValues(sysconf.values);
			monitor.log("System config restored to " + sysconf.values);
		} else {
			sysconf = confApi.getValues();
			monitor.log("System config is missing, using existing one " + sysconf);
		}
		monitor.progress(1, "System config restored");
		return sysconf;
	}
}
