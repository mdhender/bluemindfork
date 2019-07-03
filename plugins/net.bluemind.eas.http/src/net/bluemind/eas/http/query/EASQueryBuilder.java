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
package net.bluemind.eas.http.query;

import java.nio.ByteOrder;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.http.query.internal.Base64CommandCodes;
import net.bluemind.eas.http.query.internal.Base64OptParams;
import net.bluemind.eas.http.query.internal.Base64ParameterCodes;
import net.bluemind.eas.http.query.internal.RepOptParams;
import net.bluemind.vertx.common.http.BasicAuthHandler.AuthenticatedRequest;

/**
 * @author tom
 *
 */
public final class EASQueryBuilder {

	private static final Logger logger = LoggerFactory.getLogger(EASQueryBuilder.class);

	public static AuthenticatedEASQuery from(AuthenticatedRequest ar) {
		HttpServerRequest req = ar.req;
		String qs = req.query();
		if (qs == null || qs.isEmpty() || qs.contains("Cmd=") || qs.contains("User=")) {
			return simpleQuery(ar, req);
		} else {
			return base64Query(ar, qs);
		}
	}

	private static AuthenticatedEASQuery base64Query(AuthenticatedRequest ar, String qs) {
		byte[] data = java.util.Base64.getDecoder().decode(qs);
		int i = 0;
		double protocolVersion = (((float) (data[i++] & 0xff)) / 10.0); // i==0
		byte cmdCode = data[i++]; // 1

		int locale = (data[i++] << 8) + data[i++]; // i==2 and i==3

		// windows mobile 6.5 use a GUID instead of a string, so we cannot
		// create a string from those bytes directly
		String deviceId = "notDecoded";
		if (data[i] > 0) {
			byte[] devId = new byte[data[i]];
			System.arraycopy(data, i + 1, devId, 0, data[i]); // i==4
			i += data[i] + 1; // i is now on policy key size
			deviceId = java.util.Base64.getEncoder().encodeToString(devId);
		}

		Long policyKey = null;
		if (data[i++] == 4) { // got a policy key
			ByteBuf buf = Unpooled.copiedBuffer(data, i, 4);
			policyKey = buf.order(ByteOrder.LITTLE_ENDIAN).readUnsignedInt();
			i += 4;
		}
		String devType = new String(data, i + 1, data[i]);
		i += data[i] + 1;
		String command = Base64CommandCodes.getCmd(cmdCode);
		logger.info("[" + ar.login + "] protocol: " + protocolVersion + ", cmd: " + command + ", locInt: " + locale
				+ ", devId: " + deviceId + ", policy: " + policyKey + ", type: " + devType);

		Base64OptParams op = new Base64OptParams();
		op.setAcceptEncoding(ar.req.headers().get("Accept-Encoding"));
		while (data.length > i) {
			i = decodeParameters(op, data, i);
		}
		return new AuthenticatedEASQuery(ar, protocolVersion, policyKey, deviceId, devType, command, op);
	}

	private static AuthenticatedEASQuery simpleQuery(AuthenticatedRequest ar, HttpServerRequest req) {
		MultiMap params = req.params();
		MultiMap headers = req.headers();

		if (logger.isDebugEnabled()) {
			for (Entry<String, String> entry : headers.entries()) {
				logger.debug("{}: {}", entry.getKey(), entry.getValue());
			}
		}

		String pvHeader = headers.get(EasHeaders.Client.PROTOCOL_VERSION);
		double protocolVersion = 0;
		if (pvHeader != null) {
			try {
				protocolVersion = Double.parseDouble(pvHeader);
			} catch (NumberFormatException nfe) {
				logger.warn("[{}] Invalid protocol version: {}", ar.login, pvHeader);
			}
		}
		String devType = params.get("DeviceType");
		if (devType == null) {
			devType = "validate";
		} else if (devType.startsWith("IMEI")) {
			devType = headers.get("User-Agent");
		}
		String command = params.get("Cmd");

		String policyHeader = headers.get(EasHeaders.Client.POLICY_KEY);
		Long policyKey = null;
		if (policyHeader != null) {
			try {
				policyKey = Long.parseLong(policyHeader);
			} catch (NumberFormatException nfe) {
				logger.warn("[{}] Invalid policy header: {}", ar.login, policyHeader);
			}
		}
		OptionalParams op = new RepOptParams(params, headers);
		return new AuthenticatedEASQuery(ar, protocolVersion, policyKey, params.get("DeviceId"), devType, command, op);
	}

	private static int decodeParameters(Base64OptParams op, byte[] data, int i) {
		Base64ParameterCodes tag = Base64ParameterCodes.getParam(data[i++]);
		logger.info("decoding param {}", tag);
		byte length = data[i++];
		byte[] value = new byte[length];
		for (int j = 0; j < length; j++) {
			value[j] = data[i++];
		}
		switch (tag) {
		case AttachmentName:
			op.setAttachmentName(new String(value));
			break;
		case CollectionId:
			op.setCollectionId(new String(value));
			break;
		case CollectionName:
			op.setCollectionName(new String(value));
			break;
		case ItemId:
			op.setItemId(new String(value));
			break;
		case LongId:
			op.setLongId(new String(value));
			break;
		case ParentId:
			op.setParentId(new String(value));
			break;
		case Occurrence:
			op.setOccurrence(new String(value));
			break;
		case Options:
			if (value.length > 0) {
				if (value[0] == 0x01) {
					op.setSaveInSent("T");
				} else if (value[0] == 0x02) {
					op.setAcceptMultiPart("T");
				}
			}
			break;
		case User:
			break;
		default:
			break;
		}
		return i;
	}

}
