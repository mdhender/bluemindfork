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
package net.bluemind.ysnp;

/**
 * A factory to create password validators. Its primary usage is to retain
 * global objects shared by all {@link ICredentialValidator}.
 * 
 * For example reference to a connection pool used for validation should be kept
 * in implementors of {@link ICredentialValidatorFactory}.
 * 
 * 
 */
public interface ICredentialValidatorFactory {

	/**
	 * @return a validator
	 */
	ICredentialValidator getValidator();

	/**
	 * eg. bm-core
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Defines execution order when several factories are in use.
	 * 
	 * <code>0</code> means : Top Priority, runs first.
	 * <code>Integer.MAX_VALUE</code> means : low priority, run last.
	 * 
	 * @return the run priority
	 */
	int getPriority();

	/**
	 * Initializes the factory parameters with the shared configuration values
	 * 
	 * @param conf
	 */
	void init(YSNPConfiguration conf);

}
