package net.bluemind.metrics.core;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.Sets;

public enum Product {
	// JVM
	CORE("bm-core", Family.JVM,
			new String[] { "/var/spool/bm-hsm/", "/var/spool/bm-filehosting/, /var/backups/bluemind" }, false,
			"bm/core"),

	EAS("bm-eas", Family.JVM, new String[0], false, "bm/core"),

	MAPI("bm-mapi", Family.JVM, new String[] { "/var/spool/bm-mapi" }, false, "bm/core"),

	ES("bm-elasticsearch", Family.JVM, new String[] { "/var/spool/bm-elasticsearch" }, false, "bm/es"),

	HPS("bm-hps", Family.JVM, new String[0], true, "bm/hps"),

	LMTPD("bm-lmtpd", Family.JVM, new String[0], false, "bm/core"),

	LOCATOR("bm-locator", Family.JVM, new String[0], false, "bm/core"),

	MILTER("bm-milter", Family.JVM, new String[0], false, "mail/smtp", "mail/smtp-edge"),

	// As node is on all servers, it doesn't have its tag, don't forget when you
	// get
	// products by tag
	NODE("bm-node", Family.JVM, new String[] { "/tmp", "/var/log", "/" }, false),

	TIKA("bm-tika", Family.JVM, new String[0], false, "bm/core"),

	WEBSERV("bm-webserver", Family.JVM, new String[0], false, "bm/cal", "bm/ac", "bm/settings", "bm/redirector",
			"bm/webmail"),

	XMPP("bm-xmpp", Family.JVM, new String[0], false, "bm/xmpp"),

	YSNP("bm-ysnp", Family.JVM, new String[0], true, "mail/imap", "mail/smtp", "mail/smtp-edge"),

	// SYSTEM
	POSTFIX("postfix", Family.SYSTEM, new String[] { "/var/spool/postfix/" }, false, "mail/smtp", "mail/smtp-edge"),

	NGINX("bm-nginx", Family.SYSTEM, new String[0], false, "bm/nginx", "bm/nginx-edge"),

	MEMCACHED("memcached", Family.SYSTEM, new String[0], false, "bm/webmail"),

	CYRUS("cyrus", Family.SYSTEM, new String[] { "/var/spool/cyrus/data/", "/var/lib/cyrus/", "var/spool/sieve/" },
			false, "mail/imap"),

	POSTGRESQL("postgresql", Family.SYSTEM, new String[] { "/var/lib/postgresql/" }, false, "bm/pgsql",
			"bm/pgsql-data");

	public static enum Family {
		JVM, SYSTEM
	}

	private Set<String> tags;
	public final String name;
	public final Family family;
	public final String[] mountpoints;
	public final boolean useHearbeats;

	private Product(String name, Family family, String[] mountpoints, boolean useHeartbeats, String... tags) {
		this.name = name;
		this.tags = Sets.newHashSet(tags);
		this.family = family;
		this.mountpoints = mountpoints;
		this.useHearbeats = useHeartbeats;
	}

	public static Set<Product> byTag(String tag) {
		Set<Product> products = EnumSet.noneOf(Product.class);

		for (Product p : Product.values()) {
			if (p.tags.contains(tag)) {
				products.add(p);
			}
		}
		return products;
	}

	public static Product byName(String name) {
		Product product = null;

		for (Product prod : Product.values()) {
			if (prod.name.equals(name)) {
				product = prod;
				break;
			}
		}
		return product;
	}
}
