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
package net.bluemind.eas.serdes.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.provision.ProvisionRequest;
import net.bluemind.eas.dto.provision.ProvisionRequest.Policies;
import net.bluemind.eas.dto.provision.ProvisionRequest.RemoteWipe;
import net.bluemind.eas.serdes.IEasRequestParser;
import net.bluemind.eas.serdes.settings.SettingsRequestParser;

public class ProvisionRequestParser implements IEasRequestParser<ProvisionRequest> {

	private static final Logger logger = LoggerFactory.getLogger(ProvisionRequestParser.class);
	private IPreviousRequestsKnowledge request;

	@Override
	public ProvisionRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		request = past;
		ProvisionRequest pr = new ProvisionRequest();
		SettingsRequestParser srp = new SettingsRequestParser();
		Element elements = doc.getDocumentElement();
		NodeList children = elements.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "DeviceInformation":
				pr.deviceInformation = srp.parseDeviceInformation(child);
				break;
			case "Policies":
				pr.policies = parsePolicies(child);
				break;
			case "RemoteWipe":
				pr.remoteWipe = parseRemoteWipe(child);
				break;
			default:
				logger.warn("Not managed Provision child {}", child);
				break;
			}
		}

		return pr;
	}

	private Policies parsePolicies(Element el) {
		Policies policies = new Policies();
		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "Policy":
				policies.policy = parsePolicy(child);
				break;
			default:
				logger.warn("Not managed Provision.Policies child {}", child);
				break;
			}
		}

		return policies;
	}

	private Policies.Policy parsePolicy(Element el) {
		Policies.Policy policy = new Policies.Policy();
		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "PolicyType":
				policy.policyType = child.getTextContent();
				break;
			case "PolicyKey":
				policy.policyKey = child.getTextContent();
				break;
			case "Status":
				policy.status = child.getTextContent();
				break;
			default:
				logger.warn("Not managed Provision.Policies.Policy child {}", child);
				break;
			}
		}

		if (policy.policyKey == null) {
			policy.policyKey = request.getPolicyKey() != null ? request.getPolicyKey() : "0";
		}

		return policy;
	}

	private RemoteWipe parseRemoteWipe(Element el) {
		RemoteWipe rw = new RemoteWipe();
		NodeList children = el.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;

			String childName = child.getNodeName();
			switch (childName) {
			case "Status":
				rw.status = RemoteWipe.Status.get(child.getTextContent());
				break;
			default:
				logger.warn("Not managed Provision.RemoteWipe child {}", child);
				break;
			}
		}

		return rw;
	}

}
