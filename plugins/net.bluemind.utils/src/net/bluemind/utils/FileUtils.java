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
package net.bluemind.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File manipulation functions
 * 
 * 
 */
public class FileUtils {

	private static final int BUFF_SIZE = 100000;
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * File copy, from Java Performance book
	 * 
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	public static void copy(File src, File dest) throws IOException {
		logger.debug("Copying {} to {}", src.getAbsolutePath(), dest.getAbsolutePath());
		if (src.isDirectory() && dest.isDirectory()) {
			copyFolders(src, dest);
			return;
		}
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dest);
		transfer(in, out, true);
	}

	private static void copyFolders(File src, File dest) throws IOException {
		Files.walkFileTree(src.toPath(), new CopyFileVisitor(dest.toPath()));
	}

	/**
	 * Fast stream transfer method
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void transfer(InputStream in, OutputStream out, boolean closeIn) throws IOException {
		final byte[] buffer = new byte[BUFF_SIZE];

		try {
			while (true) {
				int amountRead = in.read(buffer);
				if (amountRead == -1) {
					break;
				}
				out.write(buffer, 0, amountRead);
			}
		} finally {
			if (closeIn) {
				in.close();
			}
			out.flush();
			out.close();
		}
	}

	public static String streamString(InputStream in, boolean closeIn) throws IOException {
		return new String(streamBytes(in, closeIn), "utf-8");
	}

	public static String streamString(InputStream in, boolean closeIn, Charset charset) throws IOException {
		return new String(streamBytes(in, closeIn), charset);
	}

	public static byte[] streamBytes(InputStream in, boolean closeIn) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		transfer(in, out, closeIn);
		return out.toByteArray();
	}

	public static InputStream dumpStream(InputStream in, PrintStream dump, boolean closeIn) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			transfer(in, out, closeIn);
			dump.println("-- stream dump start --");
			dump.println(out.toString());
			dump.println("-- stream dump end --");
		} catch (Exception t) {
			throw new IOException(t);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	public static void delete(File file) {
		if (file.isDirectory()) {
			for (File sFile : file.listFiles()) {
				delete(sFile);
			}
		}
		file.delete();
	}

	public static void cleanDir(File file) {
		if (file.isDirectory()) {
			for (File sFile : file.listFiles()) {
				delete(sFile);
			}
		}
	}

	public static class CopyFileVisitor extends SimpleFileVisitor<Path> {
		private final Path targetPath;
		private Path sourcePath = null;

		public CopyFileVisitor(Path targetPath) {
			this.targetPath = targetPath;
		}

		@Override
		public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
			if (sourcePath == null) {
				sourcePath = dir;
			} else {
				Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
			return FileVisitResult.CONTINUE;
		}
	}

}
