import Vue from "vue";

import router from "@bluemind/router";
import { extensions } from "@bluemind/extensions";

import * as MailAlertComponents from "./components/MailAlerts";
import * as ThreadAlertComponents from "./components/MailThread/Alerts";
import MailApp from "./components/MailApp";
import mailRoutes from "./router";
import registerAPIClients from "./registerApiClients";
import DecoratedFileItem from "./components/MailAttachment/DecoratedFileItem";
import MailViewerContent from "./components/MailViewer/MailViewerContent";

Vue.component("decorated-file-item", DecoratedFileItem);
extensions.register("webapp.mail", "file-item", {
    component: { name: "decorated-file-item", path: "message.file", priority: 0 }
});

registerAPIClients();
router.addRoutes(mailRoutes);

registerMailtoHandler();

// FIXME allow to use MailViewerContent in MessageFileViewer (avoid to import it due to circular dependency issue)
Vue.component("MailViewerContent", MailViewerContent);

Vue.component("mail-webapp", MailApp);
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
