package net.bluemind.dav.server.proto.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.SAXUtils;

public class BookMultiputQueryParser {

	private static final Logger logger = LoggerFactory.getLogger(BookMultiputQueryParser.class);

	public BookMultiputQuery parse(DavResource res, MultiMap headers, Buffer body) {
		for (String hn : headers.names()) {
			logger.info("{}: {}", hn, headers.get(hn));
		}
		logger.info("[{}][{} Bytes]\n{}", res.getPath(), body.length(), body.toString());

		BookMultiputQuery mpq = new BookMultiputQuery(res);
		BookMultiputSaxHandler sax = SAXUtils.parse(new BookMultiputSaxHandler(), body);
		mpq.setVcards(sax.getVcards());
		DavHeaders.parse(mpq, headers);
		return mpq;
	}
}
