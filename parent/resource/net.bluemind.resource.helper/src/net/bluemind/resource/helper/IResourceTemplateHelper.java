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
package net.bluemind.resource.helper;

/** Resource's template manipulations helper. */
public interface IResourceTemplateHelper {

	/**
	 * Replace the variables by appropriate values in the template associated to
	 * the given resource.
	 * 
	 * @param resourceUid
	 *            the identifier of the resource for which process the template
	 * @param localeLanguageTag
	 *            the language used for choosing a template (templates are
	 *            localized)
	 * @param organizerName
	 *            the name of the event's organizer may be used in templates as
	 *            a variable
	 * @return the resource template with variables replaced by appropriate
	 *         values
	 */
	String processTemplate(final String domainUid, final String resourceUid, final String localeLanguageTag,
			final String organizerName);

	/**
	 * @return <code>true</code> if the given <code>text</code> contains the
	 *         processed template corresponding to the given resource
	 *         identifier, <code>false</code> otherwise.
	 */
	boolean containsTemplate(final String text, final String resourceId);

	/**
	 * Remove the processed template corresponding to the given resource
	 * identifier from the given <code>text</code>.
	 */
	String removeTemplate(final String text, final String resourceId);

	/**
	 * Add the processed template to the given <code>text</code>.
	 */
	String addTemplate(final String text, final String processedTemplate);

	/** @return the HTML opening tag. */
	String tagBegin(final String resourceId);

	/** @return the HTML closing tag. */
	String tagEnd();
}
