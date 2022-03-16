package net.bluemind.tika.server.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.io.Files;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public final class ExtractTextWorker extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ExtractTextWorker.class);

	private final AutoDetectParser adp;

	private final ParseContext parseContext;

	private static final AtomicLong extractions = new AtomicLong();

	public ExtractTextWorker() {
		logger.info("Created.");
		this.parseContext = new ParseContext();
		DefaultDetector detector = new DefaultDetector();
		this.adp = new AutoDetectParser(detector);
	}

	@Override
	public void start() {
		Handler<Message<JsonObject>> handler = (Message<JsonObject> event) -> {
			try {
				JsonObject pathAndHash = event.body();
				String txt = "";
				String hash = pathAndHash.getString("hash");
				String path = pathAndHash.getString("path");
				File f = HashCache.getIfPresent(hash);
				// BM-9754 the cache seems to be able to return not-null
				if (f != null && f.exists()) {
					try {
						txt = Files.asCharSource(f, StandardCharsets.UTF_8).read();
						if (logger.isDebugEnabled()) {
							logger.debug("Used hashed value for {}", path);
						}
					} catch (IOException e) {
						logger.warn("problem with cached file, re-indexing: {}", e.getMessage());
						txt = extractToCacheFile(hash, path);
					}
				} else {
					txt = extractToCacheFile(hash, path);
				}
				long extracted = extractions.incrementAndGet();
				if ((extracted % 100) == 0) {
					logger.info("HASH cached stats: {}", HashCache.stats());
				}
				event.reply(txt);
			} catch (Exception e) {
				event.fail(500, e.getMessage());
			}
		};
		vertx.eventBus().consumer("tika.extract", handler);
	}

	private String extractToCacheFile(String hash, String path) {
		String txt;
		txt = extractText(path);
		File cachedText = new File(TikaDirectories.CACHED_TEXTS, hash + ".txt");
		try {
			Files.asCharSink(cachedText, StandardCharsets.UTF_8).write(txt);
			HashCache.put(hash, cachedText);
			if (logger.isDebugEnabled()) {
				logger.debug("Cached {} characters in {}", txt.length(), cachedText.getAbsolutePath());
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return txt;
	}

	private String extractText(String filePath) {
		logger.info("Extracting text from {}...", filePath);
		try {
			return tikaExtract(filePath);
		} catch (Exception t) {
			logger.error("Failed to parse: " + t.getMessage(), t);
			return "";
		}

	}

	private String tikaExtract(String path) throws IOException, SAXException, TikaException {
		final int limit = 20 * 1024 * 1024;
		final StringBuilder bodyTxt = new StringBuilder(1024 * 1024);
		ContentHandler saxCh = new DefaultHandler() {

			private int total;
			private boolean crAdded = false;

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				if (!crAdded) {
					bodyTxt.append('\n');
					crAdded = true;
				}
			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				total += length;
				if (total < limit) {
					bodyTxt.append(ch, start, length);
					crAdded = false;
				}
			}
		};
		Metadata md = new Metadata();
		FileInputStream in = new FileInputStream(path);
		adp.parse(in, saxCh, md, parseContext);

		return bodyTxt.toString();
	}

}
