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
package net.bluemind.cli.sysconf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.airlift.airline.Arguments;
import io.airlift.airline.Option;
import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.system.api.ISystemConfiguration;


@Command(name = "mset", description = "Set values by using a file")
public class SysconfMultipleUpdatesCommand implements ICmdLet, Runnable {
	
	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SysconfMultipleUpdatesCommand.class;
		}
	}
	
	protected CliContext ctx;
	protected CliUtils cliUtils;
	
	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}
	
	@Arguments(required = true, description = "a Json file which contains one or multiple key-value pairs")
	public String file = null;

	@Option(required = true, name = "--format", description = "a Json or Properties file which contains one or multiple key-value pairs. Format value : <json|properties>")
	public String format = null;
	
	@Override
	public void run() {
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
		
		if(new File(file).isFile()) {
			Map<String, String> map = new HashMap<>();
			
    		if (format.equalsIgnoreCase("json")){
    			map = jsonFileToMap(file);
    		}
    		if (format.equalsIgnoreCase("properties")){
    			map = propertiesFileToMap(file);
    		}
    		configurationApi.updateMutableValues(map);

		} else {
			ctx.error(String.format("%s not found", file.toString()));
		}

	}
	
	private Map<String, String> jsonFileToMap(String file){
		Map<String, String> map = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			String content = new String(Files.readAllBytes(Paths.get(file)));
	        map = mapper.readValue(content, new TypeReference<Map<String, String>>() {});
		} catch (Exception ex) {
			ctx.error(ex.getMessage());
		}
		return map;	
	}
	
	private Map<String, String> propertiesFileToMap(String file){
		Map<String, String> map = new HashMap<>();
		Properties prop = new Properties();		
		try (InputStream input = new FileInputStream(file)) {
            prop.load(input);
            map = (Map) prop;
        } catch (Exception ex) {
        	ctx.error(ex.getMessage());
        }
		
        return map;
	}
}