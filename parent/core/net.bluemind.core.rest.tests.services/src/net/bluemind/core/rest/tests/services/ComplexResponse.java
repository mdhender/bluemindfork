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
package net.bluemind.core.rest.tests.services;

public class ComplexResponse {

	private String value1Plus2;

	private String value2Plus1;

	private String subject;

	private ParamEnum paramEnum;

	public String getValue1Plus2() {
		return value1Plus2;
	}

	public void setValue1Plus2(String value1Plus2) {
		this.value1Plus2 = value1Plus2;
	}

	public String getValue2Plus1() {
		return value2Plus1;
	}

	public void setValue2Plus1(String value2Plus1) {
		this.value2Plus1 = value2Plus1;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public ParamEnum getParamEnum() {
		return paramEnum;
	}

	public void setParamEnum(ParamEnum paramEnum) {
		this.paramEnum = paramEnum;
	}

}
