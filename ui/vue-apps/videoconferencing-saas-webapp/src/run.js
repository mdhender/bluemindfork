import Vue from "vue";

import BlueMindVisioApp from "./BlueMindVisioApp.vue";
import injector from "@bluemind/inject";
import router from "@bluemind/router";
import { VideoConferencingSaasClient } from "@bluemind/videoconferencing.saas.api";

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

registerAPIClients();
registerUserSession();

Vue.component("videoconferencing-saas-webapp", BlueMindVisioApp);

router.addRoutes([
    { path: "/index.html", redirect: "/" },
    {
        name: "visio:root",
        path: "/:room*",
        component: BlueMindVisioApp
    }
]);
