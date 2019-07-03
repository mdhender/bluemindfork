package net.bluemind.tika.server.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import com.google.common.base.Throwables;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public final class BinaryFileDataHandler implements Handler<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(BinaryFileDataHandler.class);
	private File file;
	private String path;
	private long length;
	private FileOutputStream out;
	private final MessageDigest digest;

	public BinaryFileDataHandler() {
		try {
			this.file = File.createTempFile("tika", ".bin", TikaDirectories.WORK);
			this.out = new FileOutputStream(file);
			this.path = file.getAbsolutePath();
			this.digest = MessageDigest.getInstance("MD5");
		} catch (IOException | NoSuchAlgorithmException e) {
			throw Throwables.propagate(e);
		}
	}

	public void cleanup() {
		file.delete();
		logger.info("Cleaned {}bytes at {}", length, path);
	}

	public String flushAndHash() {
		try {
			out.close();
			String hash = ByteBufUtil.hexDump(Unpooled.wrappedBuffer(digest.digest())).toLowerCase();
			logger.info("Flushed {}bytes, hash => {}", length, hash);
			return hash;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void handle(Buffer event) {
		int len = event.length();
		try {
			byte[] bytes = event.getBytes();
			digest.update(bytes);
			out.write(bytes);
			length += len;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public String getFilePath() {
		return path;
	}

}
