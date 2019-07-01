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
package net.bluemind.eas.serdes.base;

import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.serdes.IResponseBuilder;

public interface IBodyOutput {

	public static final IBodyOutput DONT = new IBodyOutput() {

		@Override
		public void appendBody(IResponseBuilder builder, double protocolVersion, AppData ad,
				Callback<IResponseBuilder> done) {
			done.onResult(builder);
		}

		@Override
		public void appendAttachment(IResponseBuilder builder, double protocolVersion, AppData ad,
				Callback<IResponseBuilder> done) {
			done.onResult(builder);
		}
	};

	void appendBody(IResponseBuilder builder, double protocolVersion, AppData ad, Callback<IResponseBuilder> done);

	void appendAttachment(IResponseBuilder builder, double protocolVersion, AppData ad,
			Callback<IResponseBuilder> done);

}