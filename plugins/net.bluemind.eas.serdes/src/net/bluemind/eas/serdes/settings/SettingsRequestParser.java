/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.serdes.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.settings.SettingsRequest;
import net.bluemind.eas.dto.settings.SettingsRequest.DeviceInformation;
import net.bluemind.eas.dto.settings.SettingsRequest.DeviceInformation.Set;
import net.bluemind.eas.dto.settings.SettingsRequest.Oof;
import net.bluemind.eas.dto.settings.SettingsRequest.Oof.Get;
import net.bluemind.eas.dto.settings.SettingsRequest.UserInformation;
import net.bluemind.eas.serdes.IEasRequestParser;

public class SettingsRequestParser implements IEasRequestParser<SettingsRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SettingsRequestParser.class);

	@Override
	public SettingsRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		SettingsRequest sr = new SettingsRequest();
		Element settings = doc.getDocumentElement();
		NodeList children = settings.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "Oof": // set / get
				sr.oof = parseOof(child);
				break;
			case "DeviceInformation": // set
				sr.deviceInformation = parseDeviceInformation(child);
				break;
			case "UserInformation": // get
				sr.userInformation = parseUserInformation(child);
				break;
			default:
				logger.warn("Not managed Settings child: '{}'", child);
				break;
			}
		}
		return sr;
	}

	private UserInformation parseUserInformation(Element oofElem) {
		UserInformation ui = new UserInformation();
		NodeList children = oofElem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "Get":
				ui.get = new SettingsRequest.UserInformation.Get();
				break;
			default:
				logger.warn("Not managed Oof child: '{}'", child);
				break;
			}
		}
		return ui;
	}

	private Oof parseOof(Element oofElem) {
		Oof oof = new Oof();
		NodeList children = oofElem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;

			String childName = child.getNodeName();
			switch (childName) {
			case "Get":
				oof.get = parseOofGet(child);
				break;
			default:
				logger.warn("Not managed Oof child: '{}'", child);
				break;
			}
		}
		return oof;
	}

	public DeviceInformation parseDeviceInformation(Element diElem) {
		DeviceInformation di = new DeviceInformation();
		NodeList children = diElem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;

			String childName = child.getNodeName();
			switch (childName) {
			case "Set":
				di.set = parseDevInfSet(child);
				break;
			default:
				logger.warn("Not managed DeviceInformation child: '{}'", child);
				break;
			}
		}
		return di;
	}

	/**
	 * <pre>
	 *     <Set>
	 *       <Model>iPad3C1</Model>
	 *       <OS>iOS 9.0.1 13A404</OS>
	 *       <OSLanguage>fr-FR</OSLanguage>
	 *       <FriendlyName>Noir iPad</FriendlyName>
	 *     </Set>
	 * </pre>
	 * 
	 * @param devInfElem
	 * @return
	 */
	private Set parseDevInfSet(Element devInfElem) {
		Set set = new SettingsRequest.DeviceInformation.Set();
		NodeList children = devInfElem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;

			String childName = child.getNodeName();
			switch (childName) {
			case "Model":
				set.model = child.getTextContent();
				break;
			case "OS":
				set.os = child.getTextContent();
				break;
			case "IMEI":
				set.imei = child.getTextContent();
				break;
			case "MobileOperator":
				set.mobileOperator = child.getTextContent();
				break;
			case "UserAgent":
				set.userAgent = child.getTextContent();
				break;
			case "PhoneNumber":
				set.phoneNumber = child.getTextContent();
				break;
			case "OSLanguage":
				set.osLanguage = child.getTextContent();
				break;
			case "FriendlyName":
				set.friendlyName = child.getTextContent();
				break;
			default:
				logger.warn("Not managed Oof.Get child: '{}'", child);
				break;
			}
		}
		return set;
	}

	private Get parseOofGet(Element oofGetElem) {
		Get get = new SettingsRequest.Oof.Get();
		NodeList children = oofGetElem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "BodyType":
				get.bodyType = child.getTextContent();
				break;
			default:
				logger.warn("Not managed Oof.Get child: '{}'", child);
				break;
			}
		}
		return get;
	}

}
