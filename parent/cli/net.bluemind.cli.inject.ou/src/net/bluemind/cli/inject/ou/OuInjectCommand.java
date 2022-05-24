/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.inject.ou;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.javafaker.Beer;
import com.github.javafaker.Country;
import com.github.javafaker.Faker;
import com.github.javafaker.Pokemon;
import com.github.javafaker.Superhero;
import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.DomainNames;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member.Type;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ou", description = "Injects a batch of organizational units")
public class OuInjectCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("inject");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return OuInjectCommand.class;
		}
	}

	@Parameters(paramLabel = "<domain_name>", description = "the domain (uid or alias)", completionCandidates = DomainNames.class)
	public String domain;

	@Option(names = {
			"--levels" }, required = true, description = "number of units by tree level, levels are separated by ',' (ex: 20,10,5 create 20 root units / 10 children units by root / 5 children to each child unit)")
	public String levels;

	@Option(names = {
			"--leaf-group" }, required = false, description = "group to dispatch users to organizational units")
	public String user;

	protected CliContext ctx;
	protected CliUtils cliUtils;
	private String domainUid;
	private int[] nbNodesByLevel;
	private List<ItemValue<User>> userItems = new ArrayList<>();

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		checkParams();

		List<String> createdUids = createOuTree();

		if (userItems != null && !userItems.isEmpty()) {
			dispatchUsersToOUs(createdUids);
		}
	}

	private void dispatchUsersToOUs(List<String> ouUids) {
		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
		Random rd = new Random();

		for (ItemValue<User> userItem : userItems) {
			int i = rd.nextInt(ouUids.size() - 1);
			User userToUpdate = userItem.value;
			userToUpdate.orgUnitUid = ouUids.get(i);
			userApi.update(userItem.uid, userToUpdate);
		}
	}

	private List<String> createOuTree() {
		List<String> createdUids = new ArrayList<>();
		List<String> parentUids = new ArrayList<>();
		List<String> previousParentUids = new ArrayList<>();

		Beer beers = Faker.instance().beer();
		Pokemon pokemons = Faker.instance().pokemon();
		Superhero heros = Faker.instance().superhero();
		Country countries = Faker.instance().country();

		try {
			boolean root = true;
			for (int i = 0; i < nbNodesByLevel.length; i++) {
				int nbNodes = nbNodesByLevel[i];
				for (int j = 0; j < nbNodes; j++) {
					String ouName = "" + i + "-" + j + "_";
					if (i == 0) {
						ouName = ouName.concat(pokemons.name()).replace(" ", "_");
					} else if (i == 1) {
						ouName = ouName.concat(heros.name()).replace(" ", "_");
					} else if (i == 2) {
						ouName = ouName.concat(countries.name()).replace(" ", "_");
					} else {
						ouName = ouName.concat(beers.name()).replace(" ", "_");
					}

					if (root) {
						createOU(ouName, null).ifPresent(parentUids::add);
					} else {
						for (int p = 0; p < previousParentUids.size(); p++) {
							createOU(String.valueOf(p).concat("--").concat(ouName), previousParentUids.get(p))
									.ifPresent(parentUids::add);
						}
					}
				}
				previousParentUids.clear();
				previousParentUids.addAll(parentUids);
				createdUids.addAll(parentUids);
				parentUids.clear();
				root = false;
			}
		} catch (ServerFault e) {
			throw new CliException(e.getMessage());
		}

		return createdUids;
	}

	private Optional<String> createOU(String ouName, String parentUid) {
		IOrgUnits ouApi = ctx.adminApi().instance(IOrgUnits.class, domainUid);
		Optional<String> newUid = Optional.empty();
		try {
			String uid = UUID.randomUUID().toString();
			ouApi.create(uid, OrgUnit.create(ouName, parentUid));
			newUid = Optional.ofNullable(uid);
			ctx.info("Organization Unit " + ouName + " parent uid = " + newUid.get());
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				ctx.warn("Organization Unit " + ouName + " not created because already exists.");
				OrgUnitQuery query = new OrgUnitQuery();
				query.query = ouName;
				if (parentUid == null) {
					newUid = ouApi.search(query).stream()
							.filter(r -> r.parent == null && r.name.equalsIgnoreCase(ouName)).map(r -> r.uid)
							.findFirst();
				} else {
					newUid = ouApi.search(query).stream().filter(
							r -> r.name.equalsIgnoreCase(ouName) && r.parent != null && parentUid.equals(r.parent.uid))
							.map(r -> r.uid).findFirst();
				}
			} else {
				ctx.error("Organization Unit " + ouName + " not created because : " + sf.getMessage());
			}
		}

		return newUid;
	}

	private void checkParams() {
		domainUid = cliUtils.getDomainUidByDomain(domain);
		if (domainUid == null) {
			throw new CliException(String.format("Domain '%s' not found", domain));
		}

		if (Strings.isNullOrEmpty(levels)) {
			throw new CliException("A number of units by level must be defined (at least one for one level)");
		}

		try {
			nbNodesByLevel = Stream.of(levels.split(",")).mapToInt(Integer::parseInt).toArray();
		} catch (NumberFormatException e) {
			throw new CliException("Levels option must only contains int separated by comma");
		}

		if (nbNodesByLevel.length > 4) {
			throw new CliException("Maximum 4 levels are authorized");
		}

		if (!Strings.isNullOrEmpty(user)) {
			IGroup grpApi = ctx.adminApi().instance(IGroup.class, domainUid);
			IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
			ItemValue<Group> group = grpApi.byName(user);
			grpApi.getMembers(group.uid).stream().filter(m -> m.type == Type.user).map(m -> m.uid).forEach(u -> {
				ItemValue<User> userItem = userApi.getComplete(u);
				userItems.add(userItem);
			});
		}
	}

}
