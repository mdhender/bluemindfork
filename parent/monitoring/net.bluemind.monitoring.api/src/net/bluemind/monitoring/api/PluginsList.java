package net.bluemind.monitoring.api;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * This class is used to store every plugin information that can be monitored by
 * the API.
 *
 */
@BMApi(version = "3")
public class PluginsList {

	public Status status;

	public List<PluginInformation> pluginsList;

	public PluginsList() {

	}

	/**
	 * Adds a new plugin to the plugins list.
	 */
	public void add(PluginInformation plugin) {
		if (this.pluginsList == null) {
			this.pluginsList = new ArrayList<PluginInformation>();
		}

		this.pluginsList.add(plugin);
	}

	/**
	 * Updates the status of the plugins list.
	 */
	public void postProcess() {
		this.status = Status.UNKNOWN;
		for (PluginInformation plugin : this.pluginsList) {
			if (plugin.status.getValue() > this.status.getValue()) {
				this.status = plugin.status;
			}
		}
	}

}
