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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

	public DataProtectGeneration read() throws IOException {
		byte[] readAllBytes = Files.readAllBytes(path);
		return JsonUtils.read(new String(readAllBytes), DataProtectGeneration.class);
	}

	public void addPart(PartGeneration part) {
		logger.info("Adding part {} to generation {} of file {}", part.id, part.generationId, path.toString());
		DataProtectGeneration dpg;
		try {
			dpg = read();
		} catch (IOException e) {
			logger.warn("Cannot read generation {}: {}:{}", path, e.getClass().getName(), e.getMessage());
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
		} catch (IOException e) {
			logger.warn("Cannot read generation {}: {}:{}", path, e.getClass().getName(), e.getMessage());
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
		try (Stream<Path> files = Files.list(backupPath)) {
			return files //
					.filter(GenerationWriter::isGeneration) //
					.map(GenerationWriter::readFromPath) //
					.collect(Collectors.toList());
		} catch (IOException e) {
			logger.warn("Cannot read generation files", e);
			return Collections.emptyList();
		}
	}

	private static DataProtectGeneration readFromPath(Path path) {
		try {
			return new GenerationWriter(path).read();
		} catch (IOException e) {
			logger.warn("Cannot read generation {}:{}", path, e.getMessage());
			return null;
		}
	}

	public static void deleteGenerationFiles() {
		try (Stream<Path> files = Files.list(Paths.get(backupFolder))) {
			files.filter(GenerationWriter::isGeneration).forEach(path -> {
				logger.info("Deleting generation file {}", path.toString());
				getNodeClient().deleteFile(path.toString());
			});
		} catch (IOException e) {
			logger.warn("Cannot delete generation files", e);
		}
	}

	public static void deleteOtherGenerations(List<DataProtectGeneration> generations) {
		try (Stream<Path> files = Files.list(Paths.get(backupFolder))) {
			files.filter(GenerationWriter::isGeneration) //
					.filter(path -> {
						return notPresent(generations, path.getFileName().toString());
					}) //
					.forEach(path -> {
						logger.info("Deleting generation file {}", path.toString());
						getNodeClient().deleteFile(path.toString());
					});
		} catch (IOException e) {
			logger.warn("Cannot delete generation files", e);
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
		Paths.get(backupFolder + "/generation-" + id + ".json").toFile().delete();
	}

	private static boolean isGeneration(Path filename) {
		String name = filename.getFileName().toString();
		return name.startsWith("generation");
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
