package net.bluemind.dav.server.proto.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.SAXUtils;

public class CalMultiputQueryParser {

	private static final Logger logger = LoggerFactory.getLogger(CalMultiputQueryParser.class);

	public CalMultiputQuery parse(DavResource res, MultiMap headers, Buffer body) {
		for (String hn : headers.names()) {
			logger.info("{}: {}", hn, headers.get(hn));
		}
		logger.info("[{}][{} Bytes]\n{}", res.getPath(), body.length(), body.toString());

		CalMultiputQuery mpq = new CalMultiputQuery(res);
		CalMultiputSaxHandler sax = SAXUtils.parse(new CalMultiputSaxHandler(), body);
		mpq.setEvents(sax.getEvents());
		DavHeaders.parse(mpq, headers);
		return mpq;
	}
}
