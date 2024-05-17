package net.bluemind.cli.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.freva.asciitable.AsciiTable;

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
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class CliUtils {
	public String localTz = Optional.ofNullable(System.getProperty("net.bluemind.property.system.timezone"))
			.orElse("UTC");

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

	public Optional<ItemValue<Domain>> getDomain(String domainString, Predicate<Domain> filter) {
		String domainName = domainString;
		if (domainName != null && domainName.contains("@")) {
			domainName = domainString.split("@")[1];
		}
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		ItemValue<Domain> domain = domainService.findByNameOrAliases(domainName);

		return Optional.ofNullable(domain).filter(d -> d != null && filter.test(d.value));
	}

	public Optional<ItemValue<Domain>> getDomain(String domainString) {
		return getDomain(domainString, f -> true);
	}

	public ItemValue<Domain> getNotGlobalDomain(String domainString) {
		ItemValue<Domain> domain = getDomain(domainString, d -> d != null && !d.global)
				.orElseThrow(() -> new CliException("Domain not found or 'global.virt' not allowed"));
		return domain;
	}

	public Optional<String> getDomainUidByDomainIfPresent(String domainString) {
		if ("global.virt".equals(domainString)) {
			return Optional.of(domainString);
		}
		Optional<ItemValue<Domain>> domain = getDomain(domainString);
		return domain.map(d -> d.uid);
	}

	public List<String> getDomainUids() {
		IDomains domainService = cliContext.adminApi().instance(IDomains.class);
		return domainService.all().stream().filter(domain -> !domain.value.global).map(domain -> domain.uid)
				.collect(Collectors.toList());
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

	public String getUserUidByLogin(String domainUid, String login) {
		return getUserByLogin(domainUid, login).uid;
	}

	public String getUserLogin(String domainUid, String login) {
		return getUserByLogin(domainUid, login).value.login;
	}

	private ItemValue<User> getUserByLogin(String domainUid, String login) {
		IUser userServiceApi = cliContext.adminApi().instance(IUser.class, domainUid);
		ItemValue<User> resolved = userServiceApi.byLogin(login);
		if (resolved == null) {
			throw new CliException("user " + login + " not found");
		}
		return resolved;
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

	public String display(Map<String, String> map, String[] headers) {

		int size = map.size() + 1;

		String[][] asTable = new String[size][headers.length];

		int i = 1;
		for (Map.Entry<String, String> entry : map.entrySet()) {
			asTable[i][0] = entry.getKey();
			asTable[i][1] = entry.getValue();
			i++;
		}
		return AsciiTable.getTable(headers, asTable);
	}

	public String getAsciiTable(String[] headers, String[][] table) {
		return AsciiTable.getTable(headers, table);
	}

	/**
	 * Convert milliseconds since Unix Epoch to human readable form
	 * 
	 * @param Milliseconds since Unix Epoch
	 * @return human readable date, using yyyy-MM-dd HH:mm:ss format and local
	 *         timezone
	 */
	public String epochToLocalDate(long epoch) {
		return Instant.ofEpochMilli(epoch).atZone(ZoneId.of(localTz))
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}
}
