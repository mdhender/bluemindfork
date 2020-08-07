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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.authentication.service.internal;

import java.io.IOException;

import com.netflix.hollow.api.codegen.HollowAPIGenerator;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;

import net.bluemind.authentication.service.Token;
import net.bluemind.authentication.service.tokens.TokensStore;

public class GenerateConsumerApi {

	public static void main(String[] args) throws IOException {
		HollowWriteStateEngine writeEngine = new HollowWriteStateEngine();
		HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);
		mapper.initializeTypeState(Token.class);

		HollowAPIGenerator generator = new HollowAPIGenerator.Builder().withAPIClassname("TokensAPI").withDestination(
				"/Users/tf/dev/projects/bluemind-mapi/open/parent/authentication/net.bluemind.authentication.service/hollow-generated")
				// .withDestination(
				// "/Users/tom/git/bluemind-all/open/parent/authentication/net.bluemind.authentication.service/hollow-generated")
				.withPackageName(TokensStore.class.getPackage().getName()).withDataModel(writeEngine).build();
		// "/Users/david/devel/bluemind/open/parent/directory/net.bluemind.directory.hollow.datamodel.consumer/hollow-generated")
		// .withPackageName(Activator.class.getPackage().getName()).withDataModel(writeEngine).build();

		generator.generateSourceFiles();
	}

}
