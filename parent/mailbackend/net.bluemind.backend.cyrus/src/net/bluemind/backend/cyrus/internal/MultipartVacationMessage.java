package net.bluemind.backend.cyrus.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;

public class MultipartVacationMessage {

	private final MessageImpl message;

	public MultipartVacationMessage(String htmlText, String plainText, Charset charset) {
		BodyPart htmlPart = buildTextBodyPart(htmlText, charset, "text/html");
		BodyPart plainPart = buildTextBodyPart(plainText, charset, "text/plain");

		message = new MessageImpl();
		MultipartImpl multipart = new MultipartImpl("alternative");
		multipart.addBodyPart(plainPart);
		multipart.addBodyPart(htmlPart);
		message.setMultipart(multipart);
	}

	public String getContent() throws MimeException, IOException {
		MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.writeMessage(message, out);
		return out.toString();
	}

	private BodyPart buildTextBodyPart(String content, Charset charset, String mimeType) {
		MessageBuilder builder = new DefaultMessageBuilder();
		BodyPart part = new BodyPart();
		TextBody text = new BasicBodyFactory().textBody(content, charset);
		part.setText(text);
		Header header = builder.newHeader();
		header.setField(Fields.contentType(contentType(mimeType, charset)));
		header.setField(Fields.contentTransferEncoding("quoted-printable"));
		part.setHeader(header);
		return part;
	}

	private String contentType(String mimeType, Charset charset) {
		return mimeType + "; charset=" + charset.displayName();
	}
}
