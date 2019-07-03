/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.signature.commons.action;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing.Address;
import net.bluemind.addressbook.api.VCard.Parameter;

public class AddSignatureActionTest {

	@Test
	public void testNoReplacement() {
		String replaced = new DisclaimerVariables(getVCardSupplier()).replace(text1());
		assertEquals(text1(), replaced);
	}

	@Test
	public void testSimpleReplacement() {

		String replaced = new DisclaimerVariables(getVCardSupplier()).replace(text2());
		assertEquals(text2Replaced(), replaced);
	}

	@Test
	public void testPhoneReplacement() {

		String replaced = new DisclaimerVariables(getVCardSupplier()).replace("#{tel.work}");
		assertEquals(defaultVCard().communications.tels.get(0).value, replaced);
	}

	@Test
	public void testDecorator() {
		String replaced = new DisclaimerVariables(getVCardSupplier()).replace(text2(),
				VariableDecorators.newLineToBr());
		assertEquals("best regards\r\nBob Sinclair\r\nMy address: Office<br/>22 Accacia Avenue", replaced);
	}

	@Test
	public void testMultipleReplacementsOfSameVariable() {

		String replaced = new DisclaimerVariables(getVCardSupplier()).replace(text2Doubled());
		assertEquals(text2Replaced() + text2Replaced(), replaced);
	}

	@Test
	public void testDeleteVariablesIfVCardIsNotPresent() {

		String replaced = new DisclaimerVariables(getNoVCardSupplier()).replace(text2());
		assertEquals("best regards\r\n\r\nMy address: ", replaced);
	}

	/** Only 1 conditional block with valued variables. */
	@Test
	public void testOneOkConditionalBlock() {
		final String input = "-------\n#{gender} #{formattedName} - #{company}\n#{ne} \r\nAssistant: #{assistant} \nManager: #{manager} {/ne}\n[THEENDFOLKS]";
		final String output = new DisclaimerVariables(getVCardSupplier()).replace(input);
		System.out.format("input='%s'\noutput='%s'", input, output);
		assertEquals(
				"-------\n Bob Sinclair - Blue-mind\n \r\nAssistant: Sylvain Garcia \nManager: David Phan \n[THEENDFOLKS]",
				output);
	}

	/** 2 conditional blocks both with valued variables. */
	@Test
	public void testMultipleOkConditionalBlock() {
		final String input = "-------\n#{gender} #{formattedName} - #{company}\n#{ne} \r\nAssistant: #{assistant} \nManager: #{manager} {/ne}{ne}\r\nSidekick: #{assistant} \nChief: #{manager} {/ne}\n[THEENDFOLKS]";
		final String output = new DisclaimerVariables(getVCardSupplier()).replace(input);
		System.out.format("input='%s'\noutput='%s'", input, output);
		assertEquals(
				"-------\n Bob Sinclair - Blue-mind\n \r\nAssistant: Sylvain Garcia \nManager: David Phan \r\nSidekick: Sylvain Garcia \nChief: David Phan \n[THEENDFOLKS]",
				output);
	}

	/**
	 * 2 conditional blocks, one with valued variables, the other with one empty
	 * variable.
	 */
	@Test
	public void testMultipleOkAndPartiallyEmptyVariablesConditionalBlock() {
		final String input = "-------\n#{gender} #{formattedName} - #{company}\n#{ne} \r\nAssistant: #{assistant} \nManager: #{manager} {/ne}{ne} \r\nFax (home): #{fax.home} \nChief: #{manager} {/ne}\n[THEENDFOLKS]";
		final String output = new DisclaimerVariables(getVCardSupplier()).replace(input);
		System.out.format("input='%s'\noutput='%s'", input, output);
		assertEquals(
				"-------\n Bob Sinclair - Blue-mind\n \r\nAssistant: Sylvain Garcia \nManager: David Phan \n[THEENDFOLKS]",
				output);
	}

	/**
	 * 2 conditional blocks, one with valued variables, the other with only empty
	 * variables.
	 */
	@Test
	public void testMultipleOkAndEmptyVariablesConditionalBlock() {
		final String input = "-------\n#{gender} #{formattedName} - #{company}\n#{ne} \r\nAssistant: #{assistant} \nManager: #{manager} {/ne}{ne} \r\nFax (home): #{fax.home} \nUrl: #{url} {/ne}\n[THEENDFOLKS]";
		final String output = new DisclaimerVariables(getVCardSupplier()).replace(input);
		System.out.format("input='%s'\noutput='%s'", input, output);
		assertEquals(
				"-------\n Bob Sinclair - Blue-mind\n \r\nAssistant: Sylvain Garcia \nManager: David Phan \n[THEENDFOLKS]",
				output);
	}

	/**
	 * 2 conditional blocks, one with valued variables, the other without variables
	 * (just text).
	 */
	@Test
	public void testMultipleOkAndNoVariablesConditionalBlock() {
		final String input = "-------\n#{gender} #{formattedName} - #{company}\n#{ne} \r\nAssistant: #{assistant} \nManager: #{manager} {/ne}{ne}\nIl n'y a aucune variable{/ne}\n[THEENDFOLKS]";
		final String output = new DisclaimerVariables(getVCardSupplier()).replace(input);
		System.out.format("input='%s'\noutput='%s'", input, output);
		assertEquals(
				"-------\n Bob Sinclair - Blue-mind\n \r\nAssistant: Sylvain Garcia \nManager: David Phan \nIl n'y a aucune variable\n[THEENDFOLKS]",
				output);
	}

	/**
	 * Not providing a VCard should behave as empty variables: conditional blocks
	 * and their content should be removed when containing variables.
	 */
	@Test
	public void testConditionalBlockWithVariableIsRemovedWhenNoVCard() {
		final String input = "-------\n#{gender} #{formattedName} - #{company}\n#{ne} \r\nAssistant: #{assistant} \nManager: #{manager} {/ne}{ne}\nIl n'y a aucune variable{/ne}\n[THEENDFOLKS]";
		final String output = new DisclaimerVariables(getNoVCardSupplier()).replace(input);
		System.out.format("input='%s'\noutput='%s'", input, output);
		final String expected = "-------\n  - \n\nIl n'y a aucune variable\n[THEENDFOLKS]";
		assertEquals(expected, output);

	}

	private String text1() {
		return "Hello, I am a text without any variables\r\nMerci\r\nCiao";
	}

	private String text2() {
		return "best regards\r\n#{formattedName}\r\nMy address: #{streetAddress}";
	}

	private String text2Replaced() {
		return "best regards\r\n" + "Bob Sinclair\r\n" + "My address: Office\n22 Accacia Avenue";
	}

	private String text2Doubled() {
		return text2() + text2();
	}

	private Supplier<Optional<VCard>> getVCardSupplier() {
		return new Supplier<Optional<VCard>>() {
			@Override
			public Optional<VCard> get() {
				return Optional.of(defaultVCard());
			}

		};
	}

	private Supplier<Optional<VCard>> getNoVCardSupplier() {
		return new Supplier<Optional<VCard>>() {
			@Override
			public Optional<VCard> get() {
				return Optional.empty();
			}

		};
	}

	private VCard defaultVCard() {
		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("Bob Sinclair",
				Arrays.<VCard.Parameter>asList());

		card.related.spouse = "Clara Morgane";
		card.related.assistant = "Sylvain Garcia";
		card.related.manager = "David Phan";

		VCard.Organizational organizational = VCard.Organizational.create("Loser", "Boss", //
				VCard.Organizational.Org.create("Blue-mind", "tlse", "Dev"), //
				Arrays.<VCard.Organizational.Member>asList());

		card.organizational = organizational;

		VCard.DeliveryAddressing address = VCard.DeliveryAddressing.create(Address.create("work", "56544", "bis",
				"Office\n22 Accacia Avenue", "red", "Commonwealth", "1234", "England", Collections.emptyList()));
		card.deliveryAddressing = Arrays.asList(address);

		card.communications.tels = Arrays.asList(VCard.Communications.Tel.create("0102030405",
				Arrays.asList(Parameter.create("TYPE", "work"), Parameter.create("TYPE", "voice"))));
		return card;
	}

}
