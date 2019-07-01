/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lmtp;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lmtp.backend.IDeliveryDoneAction;
import net.bluemind.lmtp.backend.ILmtpFilterFactory;
import net.bluemind.lmtp.backend.IMessageFilter;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.bluemind.lmtp";
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	// The shared instance
	private static Activator plugin;

	private List<IDeliveryDoneAction> ddActions;

	private List<IMessageFilter> lmtpFilters;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		List<IMessageFilter> filters = getFiltersExtensions();
		this.lmtpFilters = filters;
		this.ddActions = getDeliveryDoneExtensions();

	}

	private List<IDeliveryDoneAction> getDeliveryDoneExtensions() {
		RunnableExtensionLoader<IDeliveryDoneAction> rel = new RunnableExtensionLoader<IDeliveryDoneAction>();
		List<IDeliveryDoneAction> theList = rel.loadExtensions(PLUGIN_ID, "deliverydoneaction", "action",
				"implementation");
		Collections.sort(theList, new Comparator<IDeliveryDoneAction>() {

			@Override
			public int compare(IDeliveryDoneAction o1, IDeliveryDoneAction o2) {
				return Integer.compare(o1.getPriority(), o2.getPriority());
			}
		});
		return theList;
	}

	public List<IDeliveryDoneAction> getDeliveryDoneActions() {
		return ddActions;
	}

	private List<IMessageFilter> getFiltersExtensions() {
		List<IMessageFilter> filters = new LinkedList<IMessageFilter>();

		try {
			RunnableExtensionLoader<ILmtpFilterFactory> rel = new RunnableExtensionLoader<ILmtpFilterFactory>();
			List<ILmtpFilterFactory> filtersList = rel.loadExtensions(PLUGIN_ID, "lmtpfilterfactory",
					"lmtp_filter_factory", "implementation");

			// Sort with big priorities first
			Collections.sort(filtersList, new Comparator<ILmtpFilterFactory>() {
				@Override
				public int compare(ILmtpFilterFactory f1, ILmtpFilterFactory f2) {
					return Integer.compare(f2.getPriority(), f1.getPriority());
				}
			});

			for (ILmtpFilterFactory f : filtersList) {
				IMessageFilter filter = f.getEngine();
				if (filter != null) {
					filters.add(filter);
				}
			}
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
		}

		return filters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public List<IMessageFilter> getLmtpFilters() {
		return lmtpFilters;
	}

}
