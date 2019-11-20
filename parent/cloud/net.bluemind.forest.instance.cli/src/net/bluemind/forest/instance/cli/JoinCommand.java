/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.forest.instance.cli;

import java.util.Optional;
import java.util.stream.Collectors;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.config.BmIni;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.forest.cloud.api.ForestTopology;
import net.bluemind.forest.cloud.api.IForestJoin;
import net.bluemind.forest.cloud.api.Instance;
import net.bluemind.forest.cloud.api.Instance.Node;
import net.bluemind.forest.cloud.api.Instance.Partition;
import net.bluemind.forest.cloud.api.Instance.Version;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.IInstallation;

@Command(name = "join", description = "Join bluemind instance to forest servers")
public class JoinCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("forest");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return JoinCommand.class;
		}

	}

	private CliContext ctx;

	@Arguments(title = "address", description = "Address of one forest node", required = true)
	public String address;

	@Option(name = "--alias", description = "The forest 'shared' alias", required = true)
	public String alias;

	@Override
	public void run() {
		ctx.info("Should join " + address + " with alias " + alias);

		ClientSideServiceProvider forestClient = ClientSideServiceProvider.getProvider("http://" + address + ":8089",
				null);
		IForestJoin joinApi = forestClient.instance(IForestJoin.class, alias);
		Instance instanceDesc = getInstance();
		ForestTopology topology = joinApi.handshake(instanceDesc);
		ctx.info("Received " + topology);
	}

	private Instance getInstance() {
		Instance ret = new Instance();
		ret.externalUrl = BmIni.value("external-url");
		ret.coreToken = Token.admin0();
		ret.installationId = InstallationId.getIdentifier();
		VersionInfo iv = VersionInfo.checkAndCreate(
				ctx.adminApi().instance(IInstallation.class, ret.installationId).getVersion().softwareVersion);
		ret.version = Version.create(Integer.parseInt(iv.major), Integer.parseInt(iv.minor),
				Integer.parseInt(iv.release));

		ret.aliases = ctx.adminApi().instance(IDomains.class).all().stream().filter(d -> !d.uid.equals("global.virt"))
				.map(d -> Partition.create(d.uid, d.value.aliases.toArray(new String[0]))).collect(Collectors.toList());
		ret.topology = Topology.get().nodes().stream()
				.map(n -> Node.create(n.uid, n.value.address(), n.value.tags.toArray(new String[0])))
				.collect(Collectors.toList());
		return ret;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
