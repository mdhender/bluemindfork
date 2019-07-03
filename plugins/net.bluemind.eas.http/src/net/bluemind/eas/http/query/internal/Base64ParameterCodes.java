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
package net.bluemind.eas.http.query.internal;

public enum Base64ParameterCodes {
    
	AttachmentName,//0
	CollectionId,//1
	CollectionName,//2
	ItemId,//3
	LongId,//4
	ParentId,//5
	Occurrence,//6
	Options,//7
	User;//8

	public static Base64ParameterCodes getParam(int value) {
		switch (value) {
		case 0:
			return AttachmentName;
		case 1:
			return CollectionId;
		case 2:
			return CollectionName;
		case 3:
			return ItemId;
		case 4:
			return LongId;
		case 5:
			return ParentId;
		case 6:
			return Occurrence;
		case 7:
			return Options;
		case 9:
			return User;
		default:
			return null;
		}
	}

}
