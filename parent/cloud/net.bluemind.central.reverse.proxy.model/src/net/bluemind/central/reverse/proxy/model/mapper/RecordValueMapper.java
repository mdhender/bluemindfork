package net.bluemind.central.reverse.proxy.model.mapper;

import java.util.Optional;

public interface RecordValueMapper<T> {

	Optional<InstallationInfo> mapInstallation(T rec);

	Optional<DomainInfo> mapDomain(T rec);

	Optional<DirInfo> mapDir(String domainUid, T rec);

}
