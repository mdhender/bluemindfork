import Vue from "vue";
import { TranslationRegistry } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import router from "@bluemind/router";
import { VideoConferencingSaasClient } from "@bluemind/videoconferencing.saas.api";
import BlueMindVideoApp from "./BlueMindVideoApp";
import VideoAppL10N from "../l10n";

function registerAPIClients() {
    injector.register({
        provide: "VideoConferencingService",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new VideoConferencingSaasClient(userSession.sid);
        }
    });
}

function registerUserSession() {
    injector.register({
        provide: "UserSession",
        use: window.bmcSessionInfos
    });
}

TranslationRegistry.register(VideoAppL10N);
registerAPIClients();
registerUserSession();

Vue.component("videoconferencing-saas-webapp", BlueMindVideoApp);

router.addRoutes([
    { path: "/index.html", redirect: "/" },
    {
        name: "video:root",
        path: "/:room*",
        component: BlueMindVideoApp
    }
]);
