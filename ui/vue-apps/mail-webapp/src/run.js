import Vue from "vue";

import {
    ItemsTransferClient,
    MailConversationActionsClient,
    MailConversationClient,
    OutboxClient
} from "@bluemind/backend.mail.api";
import { MailTipClient } from "@bluemind/mailmessage.api";

import injector from "@bluemind/inject";
import router from "@bluemind/router";
import store from "@bluemind/store";

import MailAlertRenderer from "./components/MailAlertRenderer";
import * as AlertComponents from "./components/MailAlerts";
import MailApp from "./components/MailApp";
import { MailboxItemsClientProxy } from "./api/APIClientsProxy";
import mailRoutes from "./router";
import MailStore from "./store/";

registerAPIClients();
store.registerModule("mail", MailStore);

router.addRoutes(mailRoutes);

Vue.component("mail-webapp", MailApp);
Vue.component("MailAlertRenderer", MailAlertRenderer);
for (let component in AlertComponents) {
    Vue.component(component, AlertComponents[component]);
}

function registerAPIClients() {
    injector.register({
        provide: "MailboxItemsPersistence",
        factory: uid => new MailboxItemsClientProxy(injector.getProvider("UserSession").get().sid, uid)
    });

    injector.register({
        provide: "OutboxPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new OutboxClient(userSession.sid, userSession.domain, userSession.userId);
        }
    });

    injector.register({
        provide: "ItemsTransferPersistence",
        factory: (sourceUid, destinationUid) => {
            const userSession = injector.getProvider("UserSession").get();
            return new ItemsTransferClient(userSession.sid, sourceUid, destinationUid);
        }
    });

    injector.register({
        provide: "MailConversationActionsPersistence",
        factory: (mailboxUid, folderUid) => {
            const userSession = injector.getProvider("UserSession").get();
            const conversationContainerId =
                "subtree_" +
                userSession.domain.replace(".", "_") +
                "!" +
                mailboxUid.replace(/^user\./, "") +
                "_conversations";
            return new MailConversationActionsClient(userSession.sid, conversationContainerId, folderUid);
        }
    });

    injector.register({
        provide: "MailConversationPersistence",
        factory: mailboxUid => {
            const userSession = injector.getProvider("UserSession").get();
            const conversationContainerId =
                "subtree_" +
                userSession.domain.replace(".", "_") +
                "!" +
                mailboxUid.replace(/^user\./, "") +
                "_conversations";
            return new MailConversationClient(userSession.sid, conversationContainerId);
        }
    });

    injector.register({
        provide: "MailTipPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new MailTipClient(userSession.sid, userSession.domain);
        }
    });
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
