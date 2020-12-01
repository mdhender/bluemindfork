/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.cli.directory.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import picocli.CommandLine.Option;

public abstract class ExportCommand extends SingleOrDomainOperation {

	@Option(names = "--output-directory", description = "The output directory path, files will be save in an email named subdirectory, default is /tmp")
	public String rootDir = "/tmp";

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	public abstract String getcontainerUid();

	public abstract String getcontainerType();

	public abstract String getFileExtension();

	public abstract void writeFile(File outputFile, String containerUid);

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws IOException {
		String outputDirectory = rootDir + "/" + de.value.email;

		File directory = new File(outputDirectory);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		List<ContainerDescriptor> containers = new ArrayList<>();
		IContainers containersService = ctx.adminApi().instance(IContainers.class);
		ContainerQuery q = ContainerQuery.ownerAndType(de.uid, getcontainerType());
		if (getcontainerUid() == null) {
			containersService.allForUser(domainUid, de.uid, q).forEach(containers::add);
		} else {
			containers.add(containersService.get(getcontainerUid()));
		}

		if (!dry) {
			for (ContainerDescriptor container : containers) {
				String filename = outputDirectory + "/" + cliUtils.encodeFilename(container.name) + getFileExtension();
				if (!dry) {
					File file = new File(filename);
					Files.deleteIfExists(file.toPath());
					writeFile(file, container.uid);
				}
				ctx.info("container " + container.uid + " of " + de.value.email + " was exported to " + filename);
			}
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}
