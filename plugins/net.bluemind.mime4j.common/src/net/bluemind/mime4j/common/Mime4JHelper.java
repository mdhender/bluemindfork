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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.field.MimeVersionFieldLenientImpl;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.stream.RecursionMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.PlatformDependent;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.mime4j.common.rewriters.impl.DontTouchHandler;
import net.bluemind.mime4j.common.rewriters.impl.XmlSafeEntityBuilder;
import net.bluemind.utils.FBOSInput;
import net.bluemind.utils.FileUtils;

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
	public static final String APP_PKCS7_SIGNATURE = "application/pkcs7-signature";

	private static final String X_BM_PARSING_OPTIONS = "X-Bm-Parsing-Options";
	private static final String ENCODED_PARTS = "encoded-parts";

	private static final Logger logger = LoggerFactory.getLogger(Mime4JHelper.class);

	private static List<AddressableEntity> expandParts(List<Entity> parts, String curAddr, int depth) {
		List<AddressableEntity> ret = new LinkedList<>();
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
		List<AddressableEntity> ret = new ArrayList<>();
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
		logger.info("*** Pristine stream with custom from: {}", from);
		MessageImpl message = new MessageImpl();
		BodyFactory bf = new BasicBodyFactory();
		return new DontTouchHandler(message, bf, from);
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
		return parse(in, message, new XmlSafeEntityBuilder(message, bf), true);
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
		serialize(toDump, out, true);
	}

	public static void serialize(Message toDump, OutputStream out, boolean encodeParts) {
		try {
			MessageWriter writer;
			if (hasEncodedHeader(toDump.getHeader())) {
				toDump.getHeader().removeFields(X_BM_PARSING_OPTIONS);
				writer = writer(false);
			} else {
				writer = writer(encodeParts);
			}
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

	private static MessageWriter writer(boolean encodeParts) {
		if (encodeParts) {
			return new DefaultMessageWriter();
		} else {
			return new DefaultMessageWriter() {
				@Override
				protected OutputStream encodeStream(OutputStream out, String encoding, boolean binaryBody)
						throws IOException {
					return out;
				}
			};
		}
	}

	public static Message parse(byte[] messageData) {
		return parse(new ByteArrayInputStream(messageData));
	}

	public static Message parse(byte[] messageData, boolean decodeEncoding) {
		return parse(new ByteArrayInputStream(messageData), decodeEncoding);
	}

	public static Message parse(InputStream in, boolean decodeEncoding) {
		return parse(in, new OffloadedBodyFactory(), decodeEncoding);
	}

	public static Message parse(InputStream in) {
		return parse(in, new OffloadedBodyFactory(), true);
	}

	public static Message parse(InputStream in, BodyFactory bf) {
		return parse(in, bf, true);
	}

	public static Message parse(InputStream in, BodyFactory bf, boolean decodeEncoding) {
		MessageImpl message = new MessageImpl();
		return parse(in, message, new DefaultEntityBuilder(message, bf), decodeEncoding);
	}

	public static boolean hasEncodedHeader(Header header) {
		Optional<Field> parsingOptionsHeader = getParsingOptionsHeader(header);
		return parsingOptionsHeader.isPresent() && parsingOptionsHeader.get().getBody().contains(ENCODED_PARTS);
	}

	public static Optional<Field> getParsingOptionsHeader(Header header) {
		if (header != null) {
			return Optional.ofNullable(header.getField(X_BM_PARSING_OPTIONS));
		}
		return Optional.empty();
	}

	private static Message parse(InputStream in, MessageImpl message, ContentHandler ch, boolean decodeEncoding) {
		MimeStreamParser parser = parser(decodeEncoding);
		parser.setContentHandler(ch);
		try {
			parser.parse(in);
		} catch (Exception e) {
			logger.error("error rewriting the email", e);
		}

		if (!decodeEncoding) {
			addParsingOptionsValue(message, ENCODED_PARTS);
		}

		return message;
	}

	private static void addParsingOptionsValue(MessageImpl message, String value) {
		Optional<Field> parsingOptionsHeader = getParsingOptionsHeader(message.getHeader());
		Set<String> bodyValues = new HashSet<>();

		parsingOptionsHeader.ifPresent(p -> {
			bodyValues
					.addAll(Splitter.on(';').omitEmptyStrings().splitToStream(p.getBody()).collect(Collectors.toSet()));
			message.getHeader().removeFields(X_BM_PARSING_OPTIONS);
		});

		bodyValues.add(value);
		RawField parsingHeaderField = new RawField(X_BM_PARSING_OPTIONS,
				bodyValues.stream().collect(Collectors.joining(";")));
		message.getHeader()
				.addField(MimeVersionFieldLenientImpl.PARSER.parse(parsingHeaderField, DecodeMonitor.SILENT));
	}

	public static MimeStreamParser parser() {
		return parser(true);
	}

	public static MimeStreamParser parser(boolean decodeEncoding) {
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

		parser.setContentDecoding(decodeEncoding);
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
		return asSizedStream(msg, true);
	}

	public static SizedStream asSizedStream(Message msg, boolean encode) throws IOException {
		FileBackedOutputStream fbos = new FileBackedOutputStream(32768, TMP_PREFIX);
		serialize(msg, fbos, encode);
		SizedStream ks = new SizedStream();
		ks.input = FBOSInput.from(fbos);
		ks.size = (int) fbos.asByteSource().size();
		return ks;
	}

	private static Optional<Boolean> parseAcceptCounters(ByteArrayOutputStream bos) throws IOException {
		try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(bos.toByteArray())))) {
			Optional<String> property = bufferedReader.lines()
					.filter(line -> line.startsWith("X-MICROSOFT-DISALLOW-COUNTER")).findFirst();
			return property.map(p -> {
				return Boolean.parseBoolean(p.substring(p.indexOf(":") + 1).trim());
			});
		}
	}

	public static InputStreamReader decodeBodyPartReader(TextBody body, Map<String, String> properties)
			throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		body.getInputStream().transferTo(outputStream);

		boolean utf8 = "us-ascii".equalsIgnoreCase(body.getMimeCharset());
		if (properties != null) {
			// X-MICROSOFT-DISALLOW-COUNTER
			parseAcceptCounters(outputStream).ifPresent(disallowCounters -> {
				properties.put("X-MICROSOFT-DISALLOW-COUNTER", Boolean.toString(disallowCounters));
			});
		}

		InputStream bodyStream = new ByteArrayInputStream(outputStream.toByteArray());

		if (utf8) {
			return new InputStreamReader(bodyStream, StandardCharsets.UTF_8);
		} else {
			return new InputStreamReader(bodyStream);
		}
	}

	public static record HashedBuffer(MappedByteBuffer buffer, String sha1, String messageId, Set<String> refs) {

		public ByteBuf nettyBuffer() {
			return Unpooled.wrappedBuffer(buffer).readerIndex(0);
		}
	}

	public static HashedBuffer mmapedEML(Message msg) throws IOException {
		return mmapedEML(msg, true);
	}

	public static HashedBuffer mmapedEML(Message msg, boolean encode) throws IOException {
		Path tmpPath = Files.createTempFile(TMP_PREFIX, ".eml");
		String hash = null;
		try (FileOutputStream out = new FileOutputStream(tmpPath.toFile()); @SuppressWarnings("deprecation")
		HashingOutputStream h = new HashingOutputStream(Hashing.sha1(), out)) {
			serialize(msg, h, encode);
			hash = h.hash().toString();
		}
		try (RandomAccessFile raf = new RandomAccessFile(tmpPath.toFile(), "r")) {
			return new HashedBuffer(raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length()), hash, msg.getMessageId(),
					refs(msg));
		} finally {
			// workaround https://bugs.openjdk.java.net/browse/JDK-4715154
			if (!IS_WINWDOWS) {
				Files.deleteIfExists(tmpPath);
			}
		}
	}

	private static Set<String> refs(Message msg) {
		Set<String> ref = new HashSet<>();
		Optional.ofNullable(msg.getHeader().getField("in-reply-to")).ifPresent(f -> ref.add(f.getBody()));
		Optional.ofNullable(msg.getHeader().getField("references")).ifPresent(refField -> //
		Splitter.on(' ').trimResults().omitEmptyStrings().splitToStream(refField.getBody()).forEach(ref::add)//
		);
		return ref;
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

	/**
	 * @param inHtml
	 * @param reply
	 * @param e
	 */
	public static String insertQuotePart(boolean inHtml, String reply, Entity e) {
		String quotePart = null;
		TextBody tb = (TextBody) e.getBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(tb.getInputStream(), out, true);
			String charset = e.getCharset();
			if (charset == null) {
				charset = "utf-8";
			}
			quotePart = new String(out.toByteArray(), charset);
		} catch (IOException ioe) {
			logger.error(ioe.getMessage(), ioe);
		}

		String replyHtml = reply;
		String replyTxt = reply;
		if (!inHtml) {
			// convert reply txt into html
			StringBuilder sb = new StringBuilder();
			String start = "<html><body><p>";
			String end = "</p></body></html>";
			sb.append(start);
			sb.append(reply.replace("\n", "<br/>"));
			sb.append(end);
			replyHtml = sb.toString();
		} else {
			replyTxt = htmlToText(reply);
		}

		if (TEXT_HTML.equals(e.getBody().getParent().getMimeType())) {
			try {
				Document doc = Jsoup.parse(replyHtml);
				Elements blockquotes = doc.getElementsByTag("blockquote");
				if (!blockquotes.isEmpty()) {
					Element blockquote = blockquotes.get(0);
					Element fragementBody = Jsoup.parseBodyFragment(quotePart).body();
					blockquote.prependChild(fragementBody);
					return doc.html();
				}

				Elements bodies = doc.getElementsByTag("body");
				if (!bodies.isEmpty()) {
					Element body = bodies.get(0);
					String blockquote = "<blockquote type=\"cite\" style=\"padding-left:5px; border-left:2px solid #1010ff; margin-left:5px\">"
							+ quotePart + "</blockquote>";
					body.append(blockquote);
					return doc.html();
				}

				return replyHtml
						+ "<blockquote type=\"cite\" style=\"padding-left:5px; border-left:2px solid #1010ff; margin-left:5px\">"
						+ quotePart + "</blockquote>";

			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				return replyHtml
						+ "<blockquote type=\"cite\" style=\"padding-left:5px; border-left:2px solid #1010ff; margin-left:5px\">"
						+ quotePart + "</blockquote>";
			}
		} else if (TEXT_PLAIN.equals(e.getBody().getParent().getMimeType())) {
			StringBuilder sb = new StringBuilder();
			sb.append(replyTxt);
			sb.append("\n");
			sb.append(formatPlainTextBodyForReplyInclusion(quotePart));
			return sb.toString();
		}

		return inHtml ? replyHtml : replyTxt;
	}

	private static String htmlToText(String replyHtml) {
		Document doc = Jsoup.parse(replyHtml);
		Document.OutputSettings outputSettings = new Document.OutputSettings();
		outputSettings.prettyPrint(false);
		doc.outputSettings(outputSettings);
		doc.select("br").before("\\n");
		doc.select("p").before("\\n");
		String str = doc.html().replace("\\\\n", "\n");
		return Jsoup.clean(str, "", Safelist.none(), outputSettings);
	}

	private static String formatPlainTextBodyForReplyInclusion(String bodyText) {
		StringBuilder sb = new StringBuilder();
		if (bodyText != null) {
			for (String line : bodyText.split("\n")) {
				sb.append(">");
				sb.append(line);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

}
