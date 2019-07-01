package net.bluemind.webmodule.devmodefilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevModeState {

	public static final class ServerPort {
		public String ip;
		public int port;
	}

	public static final class Filter {
		public String serverId;
		public String search;
		public String replace;
		public boolean active = true;
	}

	public static final class ForwardPort {
		public String serverId;
		public int src;
		public boolean active = true;
	}

	public Map<String, ServerPort> servers = new HashMap<>();

	public List<Filter> filters = new ArrayList<>();

	public List<ForwardPort> forwardPorts = new ArrayList<>();
}
