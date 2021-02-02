package net.bluemind.metrics.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.core.tick.ITickTemplateProvider;
import net.bluemind.metrics.core.tick.ITickTemplateProvider.TemplateDefinition;
import net.bluemind.metrics.core.tick.TickTemplates;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class AlertsVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(AlertsVerticle.class);

	private static final String KAPACITOR = "/usr/bin/kapacitor";

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new AlertsVerticle();
		}

	}

	@Override
	public void start() {
		EventBus eb = vertx.eventBus();
		eb.consumer("kapacitor.configuration", (Message<JsonObject> msg) -> {
			ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());

			Optional<ItemValue<Server>> kapacitor = serverApi.allComplete().stream()
					.filter(iv -> iv.value.tags.contains(TagDescriptor.bm_metrics_influx.getTag())).findFirst();

			if (!kapacitor.isPresent()) {
				logger.warn("Missing kapacitor server");
				msg.reply(new JsonObject().put("status", "ko"));
				return;
			} else {
				logger.info("Kapacitor server is {}", kapacitor.get().value.address());
			}

			ItemValue<Server> kapaSrv = kapacitor.get();

			List<ItemValue<Server>> servers = prov.instance(IServer.class, InstallationId.getIdentifier())
					.allComplete();

			serverApi.submitAndWait(kapaSrv.uid, "service kapacitor restart");
			new NetworkHelper(kapaSrv.value.address()).waitForListeningPort(9092, 10, TimeUnit.SECONDS);

			logger.info("Kapacitor re-configuration required.");
			JsonObject conf = msg.body();
			String tplId = conf.getString("templateId");
			if (tplId == null) {
				configureKapacitor(prov, kapaSrv, servers);
			} else {
				TickTemplates.template().stream().filter(tpl -> tpl.templateId().equals(tplId))
						.forEach(tpl -> loadTemplate(tpl, servers, kapaSrv, prov.getContext()));
			}
			msg.reply(new JsonObject().put("status", "ok"));
		});

	}

	private void configureKapacitor(ServerSideServiceProvider prov, ItemValue<Server> kapaSrv,
			List<ItemValue<Server>> servers) {
		List<ITickTemplateProvider> templates = TickTemplates.template();
		logger.info("Found {} tick script provider(s): {}", templates.size(),
				templates.stream().map(ITickTemplateProvider::templateId).collect(Collectors.joining(",")));

		for (ITickTemplateProvider template : templates) {
			loadTemplate(template, servers, kapaSrv, prov.getContext());
		}
	}

	private void loadTemplate(ITickTemplateProvider template, List<ItemValue<Server>> servers,
			ItemValue<Server> kapaSrv, BmContext bmContext) {
		if (logger.isInfoEnabled()) {
			logger.info("Load template {}", template.templateId());
		}
		IServer srvApi = bmContext.provider().instance(IServer.class, InstallationId.getIdentifier());
		try (InputStream in = template.content()) {
			byte[] tplContent = ByteStreams.toByteArray(in);
			srvApi.writeFile(kapaSrv.uid, "/tmp/" + template.templateId() + ".tick", tplContent);
			String cmd = KAPACITOR + " define-template " + template.templateId() + " -tick /tmp/"
					+ template.templateId() + ".tick";
			CommandStatus tplDef = srvApi.submitAndWait(kapaSrv.uid, cmd);
			if (logger.isInfoEnabled()) {
				logger.info("Template {} defined, success: {}", template.templateId(), tplDef.successful);
			}
			if (!tplDef.successful) {
				for (String s : tplDef.output) {
					if (logger.isErrorEnabled()) {
						logger.error("{}: {}", template.templateId(), s);
					}
				}
			}

			IServer serverApi = bmContext.getServiceProvider().instance(IServer.class, InstallationId.getIdentifier());
			Optional<ItemValue<Server>> core = serverApi.allComplete().stream()
					.filter(iv -> iv.value.tags.contains("bm/core")).findFirst();
			if (!core.isPresent()) {
				logger.error("Missing core");
				return;
			}
			ItemValue<Server> coreSrv = core.get();
			servers.forEach(server -> {
				String srvAddress = server.value.address();
				List<TemplateDefinition> definitions = template.createDefinitions(bmContext,
						"http://" + coreSrv.value.address() + ":8090/internal-api/alerts", server);
				logger.info("Template {} has {} definitions", template.templateId(), definitions.size());
				for (TemplateDefinition td : definitions) {
					byte[] defContent = td.variables.encode().getBytes();
					String defFilePath = "/tmp/" + template.templateId() + ".json";
					srvApi.writeFile(kapaSrv.uid, defFilePath, defContent);
					String defCmd = KAPACITOR + " define " + td.name + " -template " + template.templateId() + " -vars "
							+ defFilePath + " -dbrp telegraf.autogen";
					CommandStatus cmdRes = srvApi.submitAndWait(kapaSrv.uid, defCmd);
					logger.info("Template {} instanciated {}, for {}, success: {}", template.templateId(), td.name,
							srvAddress, cmdRes.successful);
					if (!cmdRes.successful) {
						logger.error("Template {} load error: {}", template.templateId(), cmdRes.output);
					} else {
						// TODO: remove temporary file defFilePath
						String enableCmd = KAPACITOR + " enable " + td.name;
						CommandStatus enableRes = srvApi.submitAndWait(kapaSrv.uid, enableCmd);
						logger.info("Task {} enabled: {}", td.name, enableRes.successful);
					}
				}
			});
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}
}
