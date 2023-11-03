import Vue from "vue";

import { TranslationRegistry } from "@bluemind/i18n";
import { showNotification } from "@bluemind/commons/utils/notification";
import { extensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";
import router from "@bluemind/router";
import BmRoles from "@bluemind/roles";
import WebsocketClient from "@bluemind/sockjs";

import * as MailAlertComponents from "./components/MailAlerts";
import * as ThreadAlertComponents from "./components/MailThread/Alerts";
import MailApp from "./components/MailApp";
import MailAppL10N from "../l10n/";
import mailRoutes from "./router";
import registerAPIClients from "./registerApiClients";
import registerExtensions from "./registerExtensions";
import MailViewerContent from "./components/MailViewer/MailViewerContent";

TranslationRegistry.register(MailAppL10N);
registerExtensions();
registerAPIClients();
router.addRoutes(mailRoutes);

registerMailtoHandler();
addNewMailNotification(inject("UserSession"));
// FIXME allow to use MailViewerContent in MessageFileViewer (avoid to import it due to circular dependency issue)
Vue.component("MailViewerContent", MailViewerContent);
Vue.component("MailWebapp", MailApp);
const AlertComponents = { ...MailAlertComponents, ...ThreadAlertComponents };
for (let component in AlertComponents) {
    Vue.component(component, AlertComponents[component]);
}

function registerMailtoHandler() {
    // Firefox based browsers registering popup has no "block" option, they will spam the user indefinitely
    if (window.navigator.registerProtocolHandler && !/firefox/i.test(navigator.userAgent)) {
        window.navigator.registerProtocolHandler("mailto", "/webapp/mail/%s", "Mailto Handler");
    }
}

async function addNewMailNotification(userSession) {
    if (userSession.roles.includes(BmRoles.HAS_MAIL)) {
        const application = extensions
            .get("net.bluemind.webapp", "application")
            .find(application => application.$bundle === "net.bluemind.webapp.mail.js");
        const icon = URL.createObjectURL(new Blob([application.icon.svg.toString()], { type: "image/svg+xml" }));
        const address = `${userSession.userId}.notifications.mails`;

        new WebsocketClient().register(address, async ({ data: { body: data } }) => {
            const tag = `notifications.mails`;
            const renotify = true;
            showNotification(data.title, { body: data.body, icon, badge: icon, image: icon, data, tag, renotify });
        });
    }
}
