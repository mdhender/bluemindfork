/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.service.tests.analysis;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.bluemind.content.analysis.ContentAnalyzer;

public class DummyAnalyzer implements ContentAnalyzer {

	@Override
	public CompletableFuture<Optional<String>> extractText(InputStream in) {
		return CompletableFuture.completedFuture(Optional.of("This is analyzed text"));
	}

}
