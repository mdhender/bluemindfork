package net.bluemind.central.reverse.proxy.model.common.mapper;

import java.util.Optional;

import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.MemberInfo;
import net.bluemind.central.reverse.proxy.model.common.mapper.impl.ByteArrayRecordValueMapper;

public interface RecordValueMapper<T> {

	Optional<InstallationInfo> mapInstallation(T rec);

	Optional<DomainInfo> mapDomain(T rec);

	Optional<DomainSettings> mapDomainSettings(T rec);

	Optional<DirInfo> mapDir(String domainUid, T rec);

	Optional<String> getValueUid(T rec);

	Optional<MemberInfo> mapMemberShips(T rec);

	static RecordValueMapper<byte[]> byteArray() {
		return new ByteArrayRecordValueMapper();
	}
}
