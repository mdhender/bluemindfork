package net.bluemind.webmodules.login;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.WebModule;
import net.bluemind.webmodule.server.handlers.AbstractIndexHandler;

public class UpdatePasswordHandler extends AbstractIndexHandler implements NeedVertx {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePasswordHandler.class);
	static Configuration cfg;

	public enum MsgErrorCode {
		unknown, invalidParameter, nullpassword, passwordnotmatch, usernotfound, invalidpassword, invalidnewpassword,
		emptyNewPassword, invalidCharacterNewPassword, mustNotTheSame
	}

	static {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(LoginHandler.class, "/templates");
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
	}

	@Override
	protected String getTemplateName() {
		return "updatepassword.xml";
	}

	@Override
	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		super.loadPageModel(request, model);

		String csrfToken = CSRFTokenManager.INSTANCE.initRequest(request);
		ResourceBundle resourceBundle = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", new Locale(getLang(request)));

		model.put("csrfToken", csrfToken);
		model.put("storedRequestId", "x");

		String userLogin = request.params().get("userLogin");
		if (null == userLogin) {
			userLogin = "";
		}
		model.put("userLogin", userLogin);

		manageErrorMsg(request, model, resourceBundle);

		model.put("msg", new MessageResolverMethod(resourceBundle, new Locale(getLang(request))));
		logger.debug("display login page with model {}", model);

	}

	private void manageErrorMsg(HttpServerRequest request, Map<String, Object> model, ResourceBundle resourceBundle) {
		logger.info("REQUEST: errors {}", request.uri());
		MsgErrorCode error = MsgErrorCode.unknown;

		try {
			error = MsgErrorCode.valueOf(request.params().get("authErrorCode"));
		} catch (IllegalArgumentException | NullPointerException e) {
			model.put("authErrorMsg", request.params().get("authErrorCode"));
			return;
		}

		logger.info("REQUEST: {} {}", error);

		model.put("authErrorMsg", resourceBundle.getString(String.format("updatePassword.%s", error.name())));
	}

	@Override
	protected String getLang(HttpServerRequest request) {
		String acceptLang = request.headers().get("Accept-Language");
		if (acceptLang != null && acceptLang.startsWith("fr")) {
			return "fr";
		} else {
			return "en";
		}
	}

	@Override
	public void setModule(WebModule module) {
		super.setModule(module);
	}

	@Override
	public void setVertx(Vertx vertx) {
	}
}
