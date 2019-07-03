package net.bluemind.metrics.registry;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;

public class IdFactory {

	private final Registry reg;
	private final String prefix;
	private final String product;

	/**
	 * Create an helper for consistent naming in metrics. The produced metric names
	 * will be prefixed by JVM kind or by the given object's class name.
	 * 
	 * @param reg spectator registry
	 * @param o   the class of this object is used only if we cannot identify the
	 *            JVM with the net.bluemind.property.product
	 */
	public IdFactory(Registry reg, Object o) {
		this(null, reg, o.getClass());
	}

	/**
	 * Create an helper for consistent naming in metrics. The produced metric names
	 * will be prefixed by JVM kind or by the given class name.
	 * 
	 * @param reg spectator registry
	 * @param k   used only if we cannot identify the JVM with the
	 *            net.bluemind.property.product
	 */
	public IdFactory(Registry reg, Class<?> k) {
		this(null, reg, k);
	}

	/**
	 * @param component the metric name will be prefixed jvm-kind.<em>component</em>
	 * @param reg       spectator registry
	 * @param k         used only if we cannot identify the JVM with the
	 *                  net.bluemind.property.product
	 */
	public IdFactory(String component, Registry reg, Class<?> k) {
		this.reg = reg;
		this.product = System.getProperty("net.bluemind.property.product", k.getName());
		this.prefix = product + "." + (component == null ? "" : component + ".");
	}

	/**
	 * Returns the jvm product name, usable as an origin for events
	 * 
	 * @return
	 */
	public String product() {
		return product;
	}

	public Id name(String suffix) {
		return reg.createId(prefix + suffix);
	}

	public Id name(String suffix, String... tags) {
		return reg.createId(prefix + suffix, tags);
	}
}
