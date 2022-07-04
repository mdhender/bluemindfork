import Vue from "vue";

import router from "@bluemind/router";
import store from "@bluemind/store";

import MailAlertRenderer from "./components/MailAlertRenderer";
import * as AlertComponents from "./components/MailAlerts";
import MailApp from "./components/MailApp";
import mailRoutes from "./router";
import MailStore from "./store/";
import registerAPIClients from "./registerApiClients";

registerAPIClients();
store.registerModule("mail", MailStore);

router.addRoutes(mailRoutes);

Vue.component("mail-webapp", MailApp);
Vue.component("MailAlertRenderer", MailAlertRenderer);
for (let component in AlertComponents) {
    Vue.component(component, AlertComponents[component]);
}

async function showNotification(message) {
    const result = await Notification.requestPermission();
    if (result === "granted") {
        navigator.serviceWorker.ready.then(function (registration) {
            registration.showNotification("Periodic Sync", {
                body: message
            });
        });
    }
}

(async () => {
    if ("serviceWorker" in navigator) {
        try {
            const registration = await navigator.serviceWorker.register("service-worker.js");
            // eslint-disable-next-line no-console
            console.log("Registration succeeded. Scope is " + registration.scope);
        } catch (error) {
            // eslint-disable-next-line no-console
            console.log("Registration failed with " + error);
        }

        navigator.serviceWorker.addEventListener("message", event => {
            if (event.data.type === "ERROR") {
                showNotification(event.data.payload.message);
            }
        });

        navigator.serviceWorker.addEventListener("waiting", () => {
            // eslint-disable-next-line no-console
            console.warn(
                "A new service worker has installed, but it can't activate until all tabs running the current version have fully unloaded."
            );
        });

        navigator.serviceWorker.addEventListener("installed", event => {
            if (event.isUpdate) {
                showNotification("A new version of the site is available, please refresh the page.");
            }
        });
    }
})();
