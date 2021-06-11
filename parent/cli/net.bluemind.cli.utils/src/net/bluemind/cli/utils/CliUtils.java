package net.bluemind.cli.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
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

	public static class ResolvedMailbox {
		public final String domainUid;
		public final ItemValue<Mailbox> mailbox;

		public ResolvedMailbox(String dom, ItemValue<Mailbox> resolved) {
			this.domainUid = dom;
			this.mailbox = resolved;
		}
	}

	public String getDomainUidByEmailOrDomain(String s) {
		if (s != null && s.contains("@")) {
			return getDomainUidByEmail(s);
		} else {
			return getDomainUidByDomain(s);
		}
	}

	public String getDomainUidByEmail(String email) {
		return getDomainUidByDomain(email.split("@")[1]);
	}

	public String getDomainUidByDomain(String domainString) {
		return getDomainUidByDomainIfPresent(domainString)
				.orElseThrow(() -> new CliException("Invalid or unknown domain : " + domainString));
	}

	public Optional<ItemValue<Domain>> getDomain(String domainString) {
		String domainName = domainString;
		if (domainName != null && domainName.contains("@")) {
			domainName = domainString.split("@")[1];
		}
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		ItemValue<Domain> domain = domainService.findByNameOrAliases(domainName);
		if (domain == null) {
			cliContext.error("Domain " + domainString + " not found");
		}
		return Optional.ofNullable(domain);
	}

	public Optional<String> getDomainUidByDomainIfPresent(String domainString) {
		if ("global.virt".equals(domainString)) {
			return Optional.of(domainString);
		}
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		ItemValue<Domain> domain = domainService.findByNameOrAliases(domainString);
		return Optional.ofNullable(domain).map(d -> d.uid);
	}

	public List<String> getDomainUids() {
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		return domainService.all().stream().map(domain -> domain.uid)
				.filter(domainUid -> !domainUid.equals("global.virt")).collect(Collectors.toList());
	}

	public String getUserUidByEmail(String email) {
		String domainUid = getDomainUidByEmail(email);
		IMailboxes mboxApi = cliContext.adminApi().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> resolved = mboxApi.byEmail(email);
		if (resolved == null) {
			throw new CliException("user " + email + " not found");
		}
		return resolved.uid;
	}

	public ResolvedMailbox getMailboxByEmail(String email) {
		String domainUid = getDomainUidByEmail(email);
		IMailboxes mboxApi = cliContext.adminApi().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> resolved = mboxApi.byEmail(email);
		if (resolved == null) {
			throw new CliException("user " + email + " not found");
		}
		return new ResolvedMailbox(domainUid, resolved);
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

	public Stream getStreamFromFile(Path path) {
		return getStreamFromFile(path.toString());
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
				// don't care
			}
		});
		return VertxStream.stream(stream);
	}
}
