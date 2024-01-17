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
package net.bluemind.eas.serdes.notes;

import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.notes.NotesResponse;
import net.bluemind.eas.serdes.IEasFragmentFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class NotesResponseFormatter implements IEasFragmentFormatter<NotesResponse> {

	@Override
	public void append(IResponseBuilder builder, double protocolVersion, NotesResponse response,
			Callback<IResponseBuilder> completion) {
		completion.onResult(builder);
	}

}
