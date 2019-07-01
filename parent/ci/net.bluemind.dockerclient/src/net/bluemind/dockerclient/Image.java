package net.bluemind.dockerclient;

import java.util.ArrayList;
import java.util.List;

/**
 * User: bibryam Date: 27/03/14
 */
public class Image {
	private String name;

	private String containerConfig;

	private String bindName;

	public static class Volume {
		public Volume() {

		}

		private String localPath;
		private String containerPath;

		public String getLocalPath() {
			return localPath;
		}

		public void setLocalPath(String localPath) {
			this.localPath = localPath;
		}

		public String getContainerPath() {
			return containerPath;
		}

		public void setContainerPath(String containerPath) {
			this.containerPath = containerPath;
		}
	}

	private List<Volume> volumes = new ArrayList<>();

	public String getContainerConfig() {
		return containerConfig;
	}

	public void setContainerConfig(String containerConfig) {
		this.containerConfig = containerConfig;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBindName() {
		return bindName;
	}

	public void setBindName(String bindName) {
		this.bindName = bindName;
	}

	public String getActualName() {
		if (bindName != null) {
			return bindName;
		} else {
			return name;
		}
	}

	public List<Volume> getVolumes() {
		return volumes;
	}

	public void setVolumes(List<Volume> volumes) {
		this.volumes = volumes;
	}
}
