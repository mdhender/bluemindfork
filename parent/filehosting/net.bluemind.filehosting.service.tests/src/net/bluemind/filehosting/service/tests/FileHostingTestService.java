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
package net.bluemind.filehosting.service.tests;

import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.filehosting.service.internal.FileHostingService;

public class FileHostingTestService extends FileHostingService {

	public FileHostingTestService() {
		super(null, "test.loc");
	}

	@Override
	protected List<IFileHostingService> searchExtensionPoints() {
		return Arrays.asList(Mockito.mock(IFileHostingService.class));
	}

}
