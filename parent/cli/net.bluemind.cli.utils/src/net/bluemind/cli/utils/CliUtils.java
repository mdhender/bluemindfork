package net.bluemind.cli.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class CliUtils {

	CliContext cliContext;

	public CliUtils(CliContext cliContext) {
		this.cliContext = cliContext;
	}

	public String getDomainUidFromEmailOrDomain(String s) {
		if (s != null && s.contains("@")) {
			return getDomainUidFromEmail(s);
		} else {
			return getDomainUidFromDomain(s);
		}
	}

	public String getDomainUidFromEmail(String email) {
		return getDomainUidFromDomain(email.split("@")[1]);
	}

	public String getDomainUidFromDomain(String domainString) {
		if ("global.virt".equals(domainString)) {
			return "global.virt";
		}
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		ItemValue<Domain> domain = domainService.findByNameOrAliases(domainString);
		if (domain == null) {
			throw new CliException("Invalid or unknown domain : " + domainString);
		}
		return domain.uid;
	}

	public List<String> getDomainUids() {
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		return domainService.all().stream().map(domain -> domain.uid)
				.filter(domainUid -> !domainUid.equals("global.virt")).collect(Collectors.toList());
	}

	public String getUserUidFromEmail(String email) {
		String domainUid = getDomainUidFromEmail(email);
		IMailboxes mboxApi = cliContext.adminApi().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> resolved = mboxApi.byEmail(email);
		if (resolved == null) {
			throw new CliException("user " + email + " not found");
		}
		return resolved.uid;
	}

	public String encodeFilename(String name) {
		try {
			return java.net.URLEncoder.encode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new CliException("Encoding error : " + e.getMessage());
		}
	}

	public String decodeFilename(String name) {
		try {
			return java.net.URLDecoder.decode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new CliException("Decoding error : " + e.getMessage());
		}
	}

	public Stream getStreamFromFile(String filename) {
		InputStream in;
		try {
			in = Files.newInputStream(Paths.get(filename));
		} catch (IOException e) {
			throw new CliException(e);
		}
		GenericStream<?> stream = new GenericStream<byte[]>() {

			@Override
			protected Buffer serialize(byte[] data) throws Exception {
				return Buffer.buffer(data);
			}

			@Override
			protected StreamState<byte[]> next() throws Exception {
				byte[] buffer = new byte[1024];
				int count = in.read(buffer);
				if (count == -1) {
					return StreamState.end();
				} else if (count != buffer.length) {
					byte[] data = new byte[count];
					System.arraycopy(buffer, 0, data, 0, count);
					return StreamState.data(data);
				} else {
					return StreamState.data(buffer);
				}
			}

		};
		stream.endHandler(v -> {
			try {
				in.close();
			} catch (IOException e) {
			}
		});
		return VertxStream.stream(stream);
	}
}
