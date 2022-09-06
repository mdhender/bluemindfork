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
package net.bluemind.mime4j.common;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.mime4j.stream.RecursionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.PlatformDependent;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.mime4j.common.rewriters.impl.DontTouchHandler;
import net.bluemind.mime4j.common.rewriters.impl.XmlSafeEntityBuilder;
import net.bluemind.utils.FBOSInput;

public class Mime4JHelper {
	private static final String TMP_PREFIX = System.getProperty("net.bluemind.property.product", "unknown-jvm") + "-"
			+ Mime4JHelper.class.getName();
	private static final boolean IS_WINWDOWS = PlatformDependent.isWindows();

	public static final String M_ALTERNATIVE = "multipart/alternative";
	public static final String M_SIGNED = "multipart/signed";
	public static final String M_ENCRYPTED = "multipart/encrypted";
	public static final String M_MIXED = "multipart/mixed";
	public static final String M_RELATED = "multipart/related";
	public static final String M_RELATIVE = "multipart/relative";
	public static final String TEXT_PLAIN = "text/plain";
	public static final String TEXT_CALENDAR = "text/calendar";
	public static final String TEXT_HTML = "text/html";

	private static final Logger logger = LoggerFactory.getLogger(Mime4JHelper.class);

	private static List<AddressableEntity> expandParts(List<Entity> parts, String curAddr, int depth) {
		List<AddressableEntity> ret = new LinkedList<AddressableEntity>();
		int idx = 1;
		for (Entity e : parts) {
			String mime = e.getMimeType();
			boolean mp = e.isMultipart();
			boolean alternative = M_ALTERNATIVE.equals(mime);
			boolean related = M_RELATED.equals(mime);
			boolean relative = M_RELATIVE.equals(mime);
			String currentAddress = curAddr + idx;

			// Add current part
			AddressableEntity ae = new AddressableEntity(e, currentAddress);
			ret.add(ae);

			// Add its children
			if (alternative) {
				BodyPart bp = (BodyPart) e;
				Multipart mpart = (Multipart) bp.getBody();
				List<Entity> altParts = mpart.getBodyParts();
				ret.addAll(expandAlternative(altParts, currentAddress + "."));
			} else if (mp || related || relative) {
				BodyPart bp = (BodyPart) e;
				Multipart mpart = (Multipart) bp.getBody();
				ret.addAll(expandParts(mpart.getBodyParts(), currentAddress + ".", depth + 1));
			}
			idx++;
		}

		if (logger.isDebugEnabled()) {
			for (AddressableEntity ae : ret) {
				logger.debug("{} {}", ae.getMimeAddress(), ae.getMimeType());
			}
		}

		return ret;
	}

	private static List<AddressableEntity> expandAlternative(List<Entity> altParts, String curAddr) {
		List<AddressableEntity> ret = new ArrayList<AddressableEntity>();
		int depth = 1;
		for (Entity part : altParts) {
			String addr = curAddr + depth;
			if (part.isMultipart()) {
				BodyPart bp = (BodyPart) part;
				Multipart mpart = (Multipart) bp.getBody();
				List<Entity> parts = mpart.getBodyParts();
				ret.addAll(expandAlternative(parts, curAddr + depth + "."));
			} else {
				AddressableEntity alt = new AddressableEntity(part, addr);
				ret.add(alt);
			}
			depth++;
		}
		return ret;
	}

	public static IMailRewriter untouched(Mailbox from) {
		logger.info("*** Pristine stream with custom from: " + from);
		MessageImpl message = new MessageImpl();
		BodyFactory bf = new BasicBodyFactory();
		DontTouchHandler dth = new DontTouchHandler(message, bf, from);
		return dth;
	}

	/**
	 * Parses the given mime stream and rewrites parts encoding to quoted-printable
	 * & base64. The resulting message is safe to include in utf-8 encoded XML.
	 * 
	 * @param in
	 * @return a message without xml-unsafe characters
	 */
	public static Message makeUtf8Compatible(InputStream in) {
		logger.info("*** Rewriting message parts to make them UTF-8 compatible");
		MessageImpl message = new MessageImpl();
		BodyFactory bf = new BasicBodyFactory();
		return parse(in, message, new XmlSafeEntityBuilder(message, bf));
	}

	public static void serializeBody(Body toDump, OutputStream out) {
		try {
			MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
			writer.writeBody(toDump, out);
		} catch (Exception e) {
			logger.error("Message serialization failed: " + e.getMessage(), e);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static void serialize(Message toDump, OutputStream out) {
		try {
			MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
			writer.writeMessage(toDump, out);
		} catch (Exception e) {
			logger.error("Message serialization failed: " + e.getMessage(), e);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static Message parse(byte[] messageData) {
		return parse(new ByteArrayInputStream(messageData));
	}

	public static Message parse(InputStream in) {
		return parse(in, new OffloadedBodyFactory());
	}

	public static Message parse(InputStream in, BodyFactory bf) {
		MessageImpl message = new MessageImpl();
		return parse(in, message, new DefaultEntityBuilder(message, bf));
	}

	private static Message parse(InputStream in, MessageImpl message, ContentHandler ch) {
		MimeStreamParser parser = parser();
		parser.setContentHandler(ch);
		try {
			parser.parse(in);
		} catch (Exception e) {
			logger.error("error rewriting the email", e);
		}
		return message;
	}

	public static MimeStreamParser parser() {
		MimeConfig cfg = new MimeConfig();
		cfg.setMaxHeaderLen(-1);
		cfg.setMaxHeaderCount(-1);
		cfg.setMalformedHeaderStartsBody(false);
		cfg.setMaxLineLen(-1);
		DecodeMonitor mon = DecodeMonitor.SILENT;
		BodyDescriptorBuilder bdb = new DefaultBodyDescriptorBuilder(null, LenientFieldParser.getParser(), mon);

		MimeTokenStream tokenStream = new MimeTokenStream(cfg, mon, bdb);
		tokenStream.setRecursionMode(RecursionMode.M_NO_RECURSE);
		MimeStreamParser parser = new MimeStreamParser(tokenStream);

		parser.setContentDecoding(true);
		return parser;
	}

	public static boolean isAttachment(Entity e) {
		return (e.getDispositionType() != null && "attachment".equals(e.getDispositionType()))
				|| (e.getBody() instanceof BinaryBody);
	}

	public static class SizedStream {
		public InputStream input;
		public int size;
	}

	/**
	 * Serialize to a stream AND dispose the given message.
	 * 
	 * @param msg
	 * @return
	 * @throws IOException
	 */
	public static SizedStream asSizedStream(Message msg) throws IOException {
		FileBackedOutputStream fbos = new FileBackedOutputStream(32768, TMP_PREFIX);
		serialize(msg, fbos);
		SizedStream ks = new SizedStream();
		ks.input = FBOSInput.from(fbos);
		ks.size = (int) fbos.asByteSource().size();
		return ks;
	}

	public static MappedByteBuffer mmapedEML(Message msg) throws IOException {
		Path tmpPath = Files.createTempFile(TMP_PREFIX, ".eml");
		try (FileOutputStream out = new FileOutputStream(tmpPath.toFile())) {
			serialize(msg, out);
		}
		try (RandomAccessFile raf = new RandomAccessFile(tmpPath.toFile(), "r")) {
			return raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length());
		} finally {
			// workaround https://bugs.openjdk.java.net/browse/JDK-4715154
			if (!IS_WINWDOWS) {
				Files.deleteIfExists(tmpPath);
			}
		}
	}

	/**
	 * Serialize to a stream AND dispose the given message.
	 * 
	 * @param msg
	 * @return
	 * @throws IOException
	 */
	public static InputStream asStream(Message msg) throws IOException {
		return asSizedStream(msg).input;
	}

	/**
	 * This one will keep every parts, including all inside
	 * <code>multipart/alternative
	 * containers</code>.
	 * 
	 * @param parts
	 * @return
	 */
	public static List<AddressableEntity> expandTree(List<Entity> parts) {
		if (parts.isEmpty()) {
			return new LinkedList<>();
		}
		return expandTree(parts, "", 0);
	}

	private static List<AddressableEntity> expandTree(List<Entity> parts, String curAddr, int depth) {
		List<AddressableEntity> ret = new LinkedList<AddressableEntity>();
		int idx = 1;
		for (Entity e : parts) {
			String mime = e.getMimeType();
			boolean mp = e.isMultipart();
			boolean related = M_RELATED.equals(mime);
			boolean relative = M_RELATIVE.equals(mime);
			String currentAddress = curAddr + idx;

			// Add current part
			AddressableEntity ae = new AddressableEntity(e, currentAddress);
			ret.add(ae);

			// Add its children
			if (mp || related || relative) {
				BodyPart bp = (BodyPart) e;
				Multipart mpart = (Multipart) bp.getBody();
				ret.addAll(expandParts(mpart.getBodyParts(), currentAddress + ".", depth + 1));
			}
			idx++;
		}
		return ret;
	}

}
