/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

package net.bluemind.signature.commons.action;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Parameter;

/**
 * Handles the replacement of variables in the disclaimer. Also handles
 * conditional blocks.
 */
public class DisclaimerVariables {

	private static final String CONDITIONAL_BLOCK_TAG_REGEX = "#?\\{/?ne\\}";
	private static final String VARIABLE_REGEX = "#\\{.+?\\}";
	private static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_REGEX, Pattern.DOTALL);
	private static final String CONDITIONAL_BLOCK_REGEX = "#?\\{ne\\}(.+?)#?\\{/ne\\}";
	private static final Pattern CONDITIONAL_BLOCK_PATTERN = Pattern.compile(CONDITIONAL_BLOCK_REGEX, Pattern.DOTALL);

	private Supplier<Optional<VCard>> vCardSupplier;

	public DisclaimerVariables(Supplier<Optional<VCard>> supplier) {
		this.vCardSupplier = supplier;
	}

	String uid() {
		try {
			Optional<VCard> vCard = vCardSupplier.get();
			if (vCard.isPresent()) {
				VCard card = vCard.get();
				String email = card.defaultMail();
				if (!Strings.isNullOrEmpty(email)) {
					return generateDisclaimerId(email);
				}
			}
		} catch (Exception e) {
		}
		return generateDisclaimerId();
	}

	public static String generateDisclaimerId() {
		return generateDisclaimerId("");
	}

	public static String generateDisclaimerId(String email) {
		return "x-disclaimer" + email.hashCode();
	}

	public String replace(final String input) {
		return replace(input, (key, value) -> value);
	}

	public String replace(final String input, final IVariableDecorator decorator) {
		final Optional<VCard> vCardOptional = vCardSupplier.get();
		return processAll(input, vCardOptional, decorator);
	}

	/**
	 * /!\ Improvement: in order to gain performance we may avoid to call
	 * processConditionalBlocks when no VCard is present.
	 */
	private static String processAll(final String input, final Optional<VCard> vCardOptional,
			final IVariableDecorator decorator) {
		return processVariables(processConditionalBlocks(input, vCardOptional, decorator), vCardOptional, decorator);
	}

	/**
	 * Keep or remove conditional blocks based on the variables they contain:
	 * <li>If no variable is found then keep the content</li>
	 * <li>If at least one variable is empty then remove the whole block</li>
	 * <li>If all variables are valued then keep the content</li> <br>
	 * <i>Note: the replacement of variables is not done here, for that see
	 * {@link DisclaimerVariables#processVariables(String, VCard)}</i> <br>
	 * <br>
	 * Example:<br>
	 * <li>input='<code>Name: #{formattedName}{ne}Company: #{company}{/ne}{ne} / Assistant: #{assistant} / Phone: #{tel.work}{/ne}</code>'</li>
	 * <li>If only <code>#{tel.work}</code> is empty then :
	 * output='<code>Name: #{formattedName}Company: #{company}</code>'</li>
	 * 
	 * @param input /!\ the input must contain non-replaced variables if any
	 */
	private static String processConditionalBlocks(final String input, final Optional<VCard> vCardOptional,
			final IVariableDecorator decorator) {
		String output = input;

		// 1 - remove conditional blocks containing at least one empty variable
		final Matcher matcher = CONDITIONAL_BLOCK_PATTERN.matcher(input);
		while (matcher.find()) {
			boolean deleteWholeBlock = false;

			final String blockContent = matcher.group(1);
			final Matcher variableMatcher = VARIABLE_PATTERN.matcher(blockContent);
			while (variableMatcher.find() && !deleteWholeBlock) {
				final String variable = variableMatcher.group();
				final String replacement = findReplacement(variable, vCardOptional, decorator);
				if (replacement.trim().isEmpty()) {
					deleteWholeBlock = true;
				}
			}

			if (deleteWholeBlock) {
				final String wholeMatch = matcher.group();
				output = output.replace(wholeMatch, "");
			}
		}

		// 2 - remove remaining conditional block tags
		output = output.replaceAll(CONDITIONAL_BLOCK_TAG_REGEX, "");

		return output;
	}

	/**
	 * Replace the variables by the corresponding values found in the {@link VCard}.
	 */
	private static String processVariables(final String input, final Optional<VCard> vCardOptional,
			final IVariableDecorator decorator) {
		String ouput = input;
		if (ouput.contains("#{")) {

			final Matcher matcher = VARIABLE_PATTERN.matcher(input);
			while (matcher.find()) {
				final String variable = matcher.group();
				String replacement = findReplacement(variable, vCardOptional, decorator);
				ouput = ouput.replace(variable, replacement);
			}
		}
		return ouput;
	}

	/**
	 * @return the corresponding replacement value if exists, an empty string
	 *         otherwise (never return <code>null</code>)
	 */
	private static String findReplacement(String variable, Optional<VCard> vCardOptional,
			IVariableDecorator decorator) {
		String replacement = null;

		if (vCardOptional.isPresent()) {
			final VCard vCard = vCardOptional.get();
			try {
				switch (variable) {
				case "#{formattedName}":
					replacement = vCard.identification.formatedName.value;
					break;
				case "#{gender}":
					replacement = vCard.identification.gender.value;
					break;
				case "#{name.prefixes}":
					replacement = vCard.identification.name.prefixes;
					break;
				case "#{name.suffixes}":
					replacement = vCard.identification.name.suffixes;
					break;
				case "#{name.givenNames}":
					replacement = vCard.identification.name.givenNames;
					break;
				case "#{name.familyNames}":
					replacement = vCard.identification.name.familyNames;
					break;
				case "#{name.additionalNames}":
					replacement = vCard.identification.name.additionalNames;
					break;
				case "#{email}":
					replacement = vCard.defaultMail();
					break;
				case "#{impp}":
					replacement = vCard.communications.impps.get(0).value;
					break;
				case "#{tel.home}":
					replacement = getTel(vCard.communications.tels, "home", "voice");
					break;
				case "#{tel.work}":
					replacement = getTel(vCard.communications.tels, "work", "voice");
					break;
				case "#{tel.cell}":
					replacement = getTel(vCard.communications.tels, "cell");
					break;
				case "#{fax.home}":
					replacement = getTel(vCard.communications.tels, "fax", "home");
					break;
				case "#{fax.work}":
					replacement = getTel(vCard.communications.tels, "fax", "work");
					break;
				case "#{streetAddress}":
					replacement = getAddress(vCard).address.streetAddress;
					break;
				case "#{region}":
					replacement = getAddress(vCard).address.region;
					break;
				case "#{postOfficeBox}":
					replacement = getAddress(vCard).address.postOfficeBox;
					break;
				case "#{postalCode}":
					replacement = getAddress(vCard).address.postalCode;
					break;
				case "#{locality}":
					replacement = getAddress(vCard).address.locality;
					break;
				case "#{extendedAddress}":
					replacement = getAddress(vCard).address.extentedAddress;
					break;
				case "#{countryName}":
					replacement = getAddress(vCard).address.countryName;
					break;
				case "#{url}":
					replacement = vCard.explanatory.urls.get(0).value;
					break;
				case "#{role}":
					replacement = vCard.organizational.role;
					break;
				case "#{title}":
					replacement = vCard.organizational.title;
					break;
				case "#{division}":
					replacement = vCard.organizational.org.division;
					break;
				case "#{department}":
					replacement = vCard.organizational.org.department;
					break;
				case "#{company}":
					replacement = vCard.organizational.org.company;
					break;
				case "#{assistant}":
					replacement = vCard.related.assistant;
					break;
				case "#{manager}":
					replacement = vCard.related.manager;
					break;
				default:
					replacement = "";
				}
			} catch (Exception e) {
				// do nothing ?
			}
		}

		return decorator.decorate(variable, Strings.nullToEmpty(replacement));
	}

	private static DeliveryAddressing getAddress(VCard vCard) {
		for (DeliveryAddressing addr : vCard.deliveryAddressing) {
			for (Parameter param : addr.address.parameters) {
				if (param.value.contains("work")) {
					return addr;
				}
			}
		}
		return vCard.deliveryAddressing.get(0);
	}

	private static String getTel(List<Tel> tels, String... labels) {
		List<String> labelList = Arrays.asList(labels);
		for (Tel tel : tels) {
			if (labelList.stream().allMatch(label -> tel.parameters.stream().anyMatch(p -> p.value.equals(label)))) {
				return tel.value;
			}
		}
		// fallback when there is only one matching label
		for (Tel tel : tels) {
			if (tel.parameters.stream().anyMatch(p -> p.value.equals(labelList.get(0)))) {
				return tel.value;
			}
		}
		return null;
	}

}
