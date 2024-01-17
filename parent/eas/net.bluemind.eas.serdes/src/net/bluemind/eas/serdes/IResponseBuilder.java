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
package net.bluemind.eas.serdes;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Alternative to DOM & {@link DOMUtils} for formatting the response to an EAS
 * command
 *
 */
public interface IResponseBuilder {

	IResponseBuilder start(NamespaceMapping ns);

	IResponseBuilder container(NamespaceMapping ns, String name);

	/**
	 * Creates a container with same namespace mapping as parent container
	 * 
	 * @param name
	 * @return
	 */
	IResponseBuilder container(String name);

	IResponseBuilder text(NamespaceMapping ns, String name, String value);

	IResponseBuilder text(String name, String value);

	/**
	 * Closes the active container.
	 * 
	 * @return
	 */
	IResponseBuilder endContainer();

	/**
	 * Empty element. eg. <MoreAvailable/>
	 * 
	 * @param name
	 * @return
	 */
	IResponseBuilder token(String name);

	IResponseBuilder token(NamespaceMapping ns, String name);

	void stream(NamespaceMapping ns, String name, DisposableByteSource streamable,
			Callback<IResponseBuilder> completion);

	void base64(NamespaceMapping ns, String name, DisposableByteSource streamable,
			Callback<IResponseBuilder> completion);

	void stream(String name, DisposableByteSource streamable, Callback<IResponseBuilder> completion);

	void opaqueStream(NamespaceMapping ns, String name, DisposableByteSource streamable,
			Callback<IResponseBuilder> completion);

	void end(Callback<Void> completion);

}
