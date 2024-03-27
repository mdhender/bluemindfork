package net.bluemind.metrics.core;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.server.api.TagDescriptor;

public enum Product {
	// JVM
	CORE("bm-core", Family.JVM,
			new String[] { "/var/spool/bm-hsm/", "/var/spool/bm-filehosting/, /var/backups/bluemind" }, false,
			TagDescriptor.bm_core.getTag()),

	EAS("bm-eas", Family.JVM, new String[0], false, TagDescriptor.bm_core.getTag()),

	MAPI("bm-mapi", Family.JVM, new String[] { "/var/spool/bm-mapi" }, false, TagDescriptor.bm_core.getTag()),

	ES("bm-elasticsearch", Family.JVM, new String[] { "/var/spool/bm-elasticsearch" }, false,
			TagDescriptor.bm_es.getTag()),

	MILTER("bm-milter", Family.JVM, new String[0], false, TagDescriptor.mail_smtp.getTag(),
			TagDescriptor.mail_smtp_edge.getTag()),

	// As node is on all servers, it doesn't have its tag, don't forget when you
	// get
	// products by tag
	NODE("bm-node", Family.JVM, new String[] { "/tmp", "/var/log", "/" }, false),

	TIKA("bm-tika", Family.JVM, new String[0], false, TagDescriptor.bm_core.getTag()),

	WEBSERV("bm-webserver", Family.JVM, new String[0], false, TagDescriptor.bm_calendar.getTag(),
			TagDescriptor.bm_ac.getTag(), TagDescriptor.bm_settings.getTag(), TagDescriptor.bm_redirector.getTag(),
			TagDescriptor.bm_webmail.getTag()),

	YSNP("bm-ysnp", Family.JVM, new String[0], true, TagDescriptor.mail_smtp.getTag(),
			TagDescriptor.mail_smtp_edge.getTag()),

	// SYSTEM
	POSTFIX("postfix", Family.SYSTEM, new String[] { "/var/spool/postfix/" }, false, TagDescriptor.mail_smtp.getTag(),
			TagDescriptor.mail_smtp_edge.getTag()),

	NGINX("bm-nginx", Family.SYSTEM, new String[0], false, TagDescriptor.bm_nginx.getTag(),
			TagDescriptor.bm_nginx_edge.getTag()),

	MEMCACHED("memcached", Family.SYSTEM, new String[0], false, TagDescriptor.bm_webmail.getTag()),

	POSTGRESQL("postgresql", Family.SYSTEM, new String[] { "/var/lib/postgresql/" }, false,
			TagDescriptor.bm_pgsql.getTag(), TagDescriptor.bm_pgsql_data.getTag());

	public enum Family {
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
