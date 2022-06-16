package net.bluemind.central.reverse.proxy.model.common.mapper.impl;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;

public class ByteArrayRecordValueMapper implements RecordValueMapper<byte[]> {

	private static final JsonObject EMPTY = new JsonObject();
	private static final JsonArray EMPTY_ARRAY = new JsonArray();

	@Override
	public Optional<InstallationInfo> mapInstallation(byte[] value) {
		JsonObject json = new JsonObject(new String(value));
		JsonArray tags = json.getJsonObject("value", EMPTY).getJsonArray("tags", EMPTY_ARRAY);
		boolean hasNginx = tags.getList().contains("bm/nginx");
		if (hasNginx) {
			String dataLocation = json.getString("uid");
			String ip = json.getJsonObject("value", EMPTY).getString("ip");
			if (dataLocation != null && ip != null) {
				return Optional.of(new InstallationInfo(dataLocation, ip));
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
	public Optional<DirInfo> mapDir(String domainUid, byte[] value) {
		JsonObject json = new JsonObject(new String(value));
		JsonObject dirEntryValue = json.getJsonObject("value", EMPTY).getJsonObject("entry", EMPTY);
		JsonObject dirValueValue = json.getJsonObject("value", EMPTY).getJsonObject("value", EMPTY);
		String dataLocation = dirEntryValue.getString("dataLocation");
		JsonArray emails = dirValueValue.getJsonArray("emails", EMPTY_ARRAY);
		if (dataLocation != null && emails.size() > 0) {
			Set<DirEmail> dirEmails = emails.stream().map(JsonObject.class::cast)
					.map(email -> new DirEmail(email.getString("address"), email.getBoolean("allAliases")))
					.collect(Collectors.toSet());
			return Optional.of(new DirInfo(domainUid, dirEmails, dataLocation));
		}
		return Optional.empty();
	}

}
