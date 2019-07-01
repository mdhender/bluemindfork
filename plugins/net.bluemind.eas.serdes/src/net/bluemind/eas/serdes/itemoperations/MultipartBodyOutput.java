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
package net.bluemind.eas.serdes.itemoperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.base.IBodyOutput;

public class MultipartBodyOutput implements IBodyOutput {

	private int partNumber;
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MultipartBodyOutput.class);

	public MultipartBodyOutput() {
		partNumber = 1;
	}

	@Override
	public void appendBody(IResponseBuilder builder, double protocolVersion, AppData ad,
			Callback<IResponseBuilder> done) {
		builder.text(NamespaceMapping.ItemOperations, "Part", Integer.toString(partNumber++));
		done.onResult(builder);
	}

	@Override
	public void appendAttachment(IResponseBuilder builder, double protocolVersion, AppData ad,
			Callback<IResponseBuilder> done) {
		// builder.container(NamespaceMapping.ItemOperations, "Data");
		builder.text(NamespaceMapping.ItemOperations, "Part", Integer.toString(partNumber++));
		// builder.endContainer();
		done.onResult(builder);
	}

}
