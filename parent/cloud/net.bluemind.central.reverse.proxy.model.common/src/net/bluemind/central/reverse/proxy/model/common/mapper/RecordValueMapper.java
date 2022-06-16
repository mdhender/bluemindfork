package net.bluemind.central.reverse.proxy.model.common.mapper;

import java.util.Optional;

import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.mapper.impl.ByteArrayRecordValueMapper;

public interface RecordValueMapper<T> {

	Optional<InstallationInfo> mapInstallation(T rec);

	Optional<DomainInfo> mapDomain(T rec);

	Optional<DirInfo> mapDir(String domainUid, T rec);

	static RecordValueMapper<byte[]> byteArray() {
		return new ByteArrayRecordValueMapper();
	}
}
