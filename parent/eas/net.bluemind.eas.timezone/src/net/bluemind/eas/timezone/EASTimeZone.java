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
package net.bluemind.eas.timezone;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.vertx.core.json.JsonObject;

public class EASTimeZone {

	public final int bias;
	public final String standardName;
	public final SystemTime standardDate;
	public final int standardBias;
	public final String daylightName;
	public final SystemTime daylightDate;
	public final int daylightBias;

	public EASTimeZone(int bias, String standardName, SystemTime standardDate, int standardBias, String daylightName,
			SystemTime daylightDate, int daylightBias) {
		this.bias = bias;
		this.standardName = standardName;
		this.standardDate = standardDate;
		this.standardBias = standardBias;
		this.daylightName = daylightName;
		this.daylightDate = daylightDate;
		this.daylightBias = daylightBias;
	}

	public String toBase64() {
		ByteBuf out = Unpooled.buffer().order(ByteOrder.LITTLE_ENDIAN);
		out.writeInt(bias);
		byte[] utf = standardName.getBytes(StandardCharsets.UTF_16LE);
		out.writeBytes(utf);
		out.writeZero(64 - utf.length);
		standardDate.writeTo(out);
		out.writeInt(standardBias);

		utf = daylightName.getBytes(StandardCharsets.UTF_16LE);
		out.writeBytes(utf);
		out.writeZero(64 - utf.length);
		daylightDate.writeTo(out);
		out.writeInt(daylightBias);

		ByteBuf encoded = Base64.encode(out, false);
		String ascii = encoded.toString(StandardCharsets.US_ASCII);
		encoded.release();
		return ascii;
	}

	public JsonObject toJson() {
		JsonObject ret = new JsonObject();
		ret.put("bias", bias);
		ret.put("standardName", standardName);
		ret.put("standardDate", standardDate.toJson());
		ret.put("standardBias", standardBias);
		ret.put("daylightName", daylightName);
		ret.put("daylightDate", daylightDate.toJson());
		ret.put("daylightBias", daylightBias);
		return ret;
	}

	public String toString() {
		return toJson().encodePrettily();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bias;
		result = prime * result + daylightBias;
		result = prime * result + ((daylightDate == null) ? 0 : daylightDate.hashCode());
		result = prime * result + standardBias;
		result = prime * result + ((standardDate == null) ? 0 : standardDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EASTimeZone other = (EASTimeZone) obj;
		if (bias != other.bias)
			return false;
		if (daylightBias != other.daylightBias)
			return false;
		if (daylightDate == null) {
			if (other.daylightDate != null)
				return false;
		} else if (!daylightDate.equals(other.daylightDate))
			return false;
		if (standardBias != other.standardBias)
			return false;
		if (standardDate == null) {
			if (other.standardDate != null)
				return false;
		} else if (!standardDate.equals(other.standardDate))
			return false;
		return true;
	}

}
