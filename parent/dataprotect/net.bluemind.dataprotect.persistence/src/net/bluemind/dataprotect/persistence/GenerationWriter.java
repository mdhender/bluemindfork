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
package net.bluemind.dataprotect.persistence;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class GenerationWriter {
	private static final String backupFolder = "/var/backups/bluemind";
	private static final Logger logger = LoggerFactory.getLogger(GenerationWriter.class);

	private final Path path;

	public GenerationWriter(int id) {
		this.path = Paths.get(backupFolder + "/generation-" + id + ".json");
	}

	public GenerationWriter(Path path) {
		this.path = path;
	}

	public void write(DataProtectGeneration dpg) {
		try {
			logger.info("Writing generation file {}", path.toString());
			getNodeClient().writeFile(path.toString(), new ByteArrayInputStream(JsonUtils.asString(dpg).getBytes()));
		} catch (ServerFault sf) {
			logger.warn("Cannot write generation {}:{}", path, sf.getMessage());
			throw sf;
		}
	}

	public DataProtectGeneration read() {
		byte[] readAllBytes = getNodeClient().read(path.toAbsolutePath().toString());
		return JsonUtils.read(new String(readAllBytes), DataProtectGeneration.class);
	}

	public void addPart(PartGeneration part) {
		logger.info("Adding part {} to generation {} of file {}", part.id, part.generationId, path);
		DataProtectGeneration dpg;
		try {
			dpg = read();
		} catch (ServerFault sf) {
			logger.warn("Cannot read generation {}: {}", path, sf.getMessage());
			return;
		}
		dpg.parts.add(part);
		write(dpg);
	}

	public void updatePart(PartGeneration part) {
		logger.info("Updating part {} of generation {} of file {}", part.id, part.generationId, path.toString());
		DataProtectGeneration dpg;
		try {
			dpg = read();
		} catch (ServerFault sf) {
			logger.warn("Cannot read generation {}: {}", path, sf.getMessage());
			return;
		}
		dpg.parts.remove(part);
		dpg.parts.add(part);
		write(dpg);
	}

	public static List<DataProtectGeneration> readGenerationFiles() {
		Path backupPath = Paths.get(backupFolder);
		if (!backupPath.toFile().exists()) {
			return Collections.emptyList();
		}
		var nodeClient = getNodeClient();
		try {
			return nodeClient.listFiles(backupPath.toString()).stream().filter(GenerationWriter::isGeneration) //
					.map(GenerationWriter::readFromPath) //
					.toList();
		} catch (ServerFault sf) {
			logger.warn("Cannot read generation files: {}", sf.getMessage());
			return Collections.emptyList();
		}

	}

	private static DataProtectGeneration readFromPath(FileDescription fd) {
		try {
			return new GenerationWriter(Paths.get(fd.getPath())).read();
		} catch (ServerFault sf) {
			logger.warn("Cannot read generation {}: {}", fd.getPath(), sf.getMessage());
			return null;
		}
	}

	public static void deleteGenerationFiles() {
		var nodeClient = getNodeClient();
		try {
			Stream<FileDescription> files = nodeClient.listFiles(Paths.get(backupFolder).toString()).stream();
			files.filter(GenerationWriter::isGeneration).forEach(fd -> {
				logger.info("Deleting generation file {}", fd);
				getNodeClient().deleteFile(fd.getPath());
			});
		} catch (ServerFault sf) {
			logger.warn("Cannot delete generation files: {}", sf.getMessage());
		}
	}

	public static void deleteOtherGenerations(List<DataProtectGeneration> generations) {
		var nodeClient = getNodeClient();
		try {
			Stream<FileDescription> files = nodeClient.listFiles(Paths.get(backupFolder).toString()).stream();
			files.filter(GenerationWriter::isGeneration) //
					.filter(fd -> {
						return notPresent(generations, fd.getName());
					}) //
					.forEach(fd -> {
						logger.info("Deleting generation file {}", fd.getPath());
						nodeClient.deleteFile(fd.getPath());
					});
		} catch (ServerFault sf) {
			logger.warn("Cannot delete generation files: {}", sf.getMessage());
		}
	}

	private static boolean notPresent(List<DataProtectGeneration> generations, String filename) {
		for (DataProtectGeneration dataProtectGeneration : generations) {
			String gFileName = "generation-" + dataProtectGeneration.id + ".json";
			if (gFileName.equals(filename)) {
				return false;
			}
		}
		return true;
	}

	public static void deleteGenerationFile(int id) {
		getNodeClient().deleteFile(Paths.get(backupFolder + "/generation-" + id + ".json").toAbsolutePath().toString());
	}

	private static boolean isGeneration(FileDescription fd) {
		return fd.getName().startsWith("generation");
	}

	private static INodeClient getNodeClient() {
		IServer srvApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		Optional<ItemValue<Server>> coreServer = srvApi.allComplete().stream()
				.filter(s -> s.value.tags.contains("bm/core")).findFirst();

		if (!coreServer.isPresent()) {
			throw new ServerFault("Unable to find server tagged as bm/core");
		}

		return NodeActivator.get(coreServer.get().value.ip);
	}
}
