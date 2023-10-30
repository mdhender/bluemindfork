package net.bluemind.central.reverse.proxy.model.common.mapper.impl;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.MemberInfo;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;

public class ByteArrayRecordValueMapper implements RecordValueMapper<byte[]> {
	private final Logger logger = LoggerFactory.getLogger(ByteArrayRecordValueMapper.class);

	private static final JsonObject EMPTY = new JsonObject();
	private static final JsonArray EMPTY_ARRAY = new JsonArray();

	@Override
	public Optional<InstallationInfo> mapInstallation(byte[] value) {
		JsonObject json = new JsonObject(new String(value));
		JsonArray tags = json.getJsonObject("value", EMPTY).getJsonArray("tags", EMPTY_ARRAY);
		boolean hasNginx = tags.getList().contains("bm/nginx");
		boolean hasCore = tags.getList().contains("bm/core");
		if (hasNginx || hasCore) {
			String dataLocation = json.getString("uid");
			String ip = json.getJsonObject("value", EMPTY).getString("ip");
			if (dataLocation != null && ip != null) {
				return Optional.of(new InstallationInfo(dataLocation, ip, hasNginx, hasCore));
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<DomainInfo> mapDomain(byte[] value) {
		JsonObject json = new JsonObject(new String(value));
		String uid = json.getString("uid");
		Set<String> aliases = json.getJsonObject("value", EMPTY).getJsonArray("aliases", EMPTY_ARRAY).stream()
				.map(String.class::cast).collect(Collectors.toSet());
		return Optional.of(new DomainInfo(uid, aliases));
	}

	@Override
	public Optional<DomainSettings> mapDomainSettings(byte[] value) {
		JsonObject json = new JsonObject(new String(value));
		String domainUid = json.getString("uid");
		JsonObject settings = json.getJsonObject("value", EMPTY).getJsonObject("settings", EMPTY);

		String mailRoutingRelay = settings.getString("mail_routing_relay");
		if (Strings.isNullOrEmpty(mailRoutingRelay)) {
			mailRoutingRelay = null;
		}
		boolean mailForwardUnknown = Boolean.parseBoolean(settings.getString("mail_forward_unknown_to_relay"));

		logger.info("settings: {}", new DomainSettings(domainUid, mailRoutingRelay, mailForwardUnknown));
		return Optional.of(new DomainSettings(domainUid, mailRoutingRelay, mailForwardUnknown));
	}

	@Override
	public Optional<DirInfo> mapDir(String domainUid, byte[] value) {
		JsonObject json = new JsonObject(new String(value));
		JsonObject dirEntryValue = json.getJsonObject("value", EMPTY).getJsonObject("entry", EMPTY);
		JsonObject dirValueMailbox = json.getJsonObject("value", EMPTY).getJsonObject("mailbox", EMPTY);
		JsonObject dirValueValue = json.getJsonObject("value", EMPTY).getJsonObject("value", EMPTY);
		String entryUid = dirEntryValue.getString("entryUid");
		boolean archived = Boolean.parseBoolean(dirEntryValue.getString("archived"));
		String kind = dirEntryValue.getString("kind");
		String dataLocation = dirEntryValue.getString("dataLocation");
		JsonArray emails = dirValueValue.getJsonArray("emails", EMPTY_ARRAY);
		String mailboxName = dirValueMailbox.getString("name");
		String routing = dirValueMailbox.getString("routing");

		Set<DirEmail> dirEmails = emails.stream().map(JsonObject.class::cast)
				.map(email -> new DirEmail(email.getString("address"), email.getBoolean("allAliases")))
				.collect(Collectors.toSet());
		return Optional
				.of(new DirInfo(domainUid, entryUid, kind, archived, mailboxName, routing, dirEmails, dataLocation));
	}

	@Override
	public Optional<String> getValueUid(byte[] value) {
		return Optional.ofNullable(new JsonObject(new String(value)).getString("uid"));
	}

	@Override
	public Optional<MemberInfo> mapMemberShips(byte[] value) {
		JsonObject json = new JsonObject(new String(value));

		boolean added = Boolean.parseBoolean(json.getJsonObject("value", EMPTY).getString("added"));
		String groupUid = json.getString("uid");
		String memberType = json.getJsonObject("value", EMPTY).getJsonObject("member", EMPTY).getString("type");
		String memberUid = json.getJsonObject("value", EMPTY).getJsonObject("member", EMPTY).getString("uid");

		if (groupUid == null || memberType == null || memberUid == null) {
			return Optional.empty();
		}

		return Optional.of(new MemberInfo(added, groupUid, memberType, memberUid));
	}
}
