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
package net.bluemind.directory.hollow.datamodel.producer;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class Value {

	public static final NullValue NULL = new NullValue();

	public final Object data;

	public Value(Object data) {
		this.data = data;
	}

	public String toString() {
		return data.toString();
	}

	public Date toDate() {
		throw new UnsupportedOperationException();
	}

	public byte[] toByteArray() {
		throw new UnsupportedOperationException();
	}

	public int toInt() {
		throw new UnsupportedOperationException();
	}

	public boolean toBoolean() {
		throw new UnsupportedOperationException();
	}

	public List<?> toList() {
		return Collections.emptyList();
	}

	public static class IntValue extends Value {

		public IntValue(int data) {
			super(data);
		}

		@Override
		public int toInt() {
			return (int) data;
		}

	}

	public static class StringValue extends Value {

		public StringValue(String data) {
			super(data);
		}

		@Override
		public String toString() {
			return (String) data;
		}

	}

	public static class ByteArrayValue extends Value {

		public ByteArrayValue(byte[] data) {
			super(data);
		}

		@Override
		public byte[] toByteArray() {
			return (byte[]) data;
		}

	}

	public static class DateValue extends Value {

		public DateValue(Date data) {
			super(data);
		}

		@Override
		public Date toDate() {
			return (Date) data;
		}

	}

	public static class ListValue extends Value {

		public ListValue(List<?> data) {
			super(data);
		}

		public List<?> toList() {
			return (List<?>) data;
		}

	}

	public static class NullValue extends Value {

		public NullValue() {
			super(null);
		}

		public String toString() {
			return null;
		}

		public byte[] toByteArray() {
			return null;
		}

		public int toInt() {
			return 0;
		}

		public Date toDate() {
			return null;
		}

	}

	public static class BooleanValue extends Value {

		public BooleanValue(boolean data) {
			super(data);
		}

		@Override
		public boolean toBoolean() {
			return (Boolean) data;
		}

	}
}
