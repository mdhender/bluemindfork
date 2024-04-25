package net.bluemind.configfile.imap;

public class ImapConfig {

	public static final String OVERRIDE_PATH = "/etc/bm/imap.conf";

	public static final String PORT = "imap.port";
	public static final String PROXY_PROTOCOL = "imap.proxy-protocol";
	public static final String IDLE_TIMEOUT = "imap.idle-timeout";
	public static final String CHUNK_SIZE = "imap.chunk-size";
	public static final String TCP_NODELAY = "imap.tcp-nodelay";
	public static final String TCP_CORK = "imap.tcp-cork";

	public static class Throughput {
		private Throughput() {

		}

		public static final String KEY = "imap.throughput";

		public static final String STRATEGY = "imap.throughput.strategy";
		public static final String CAPACITY = "imap.throughput.capacity";
		public static final String PERIOD = "imap.throughput.period";
		public static final String LOG_PERIOD = "imap.throughput.log-period";
		public static final String BYPASS = "imap.throughput.bypass";
	}

}
