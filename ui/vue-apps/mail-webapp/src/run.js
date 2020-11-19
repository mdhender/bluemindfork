import Vue from "vue";
import { AddressBooksClient } from "@bluemind/addressbook.api";
import { CalendarClient } from "@bluemind/calendar.api";
import { ContainersClient } from "@bluemind/core.container.api";
import { ItemsTransferClient } from "@bluemind/backend.mail.api";
import { MailboxesClient } from "@bluemind/mailbox.api";
import { OutboxClient } from "@bluemind/backend.mail.api";
import { OwnerSubscriptionsClient } from "@bluemind/core.container.api";
import { UserMailIdentitiesClient } from "@bluemind/user.api";
import { TaskClient } from "@bluemind/core.task.api";
import injector from "@bluemind/inject";
import router from "@bluemind/router";
import store from "@bluemind/store";
import MailAlertRenderer from "./components/MailAlertRenderer";
import * as AlertComponents from "./components/MailAlerts";
import MailApp from "./components/MailApp";
import { MailboxItemsClientProxy, MailboxFoldersClientProxy } from "./store.deprecated/mailbackend/APIClientsProxy";
import mailRoutes from "./router";
import Scheduler from "./scheduler";
import MailStore from "./store/";
import MailWebAppStore from "./store.deprecated/";
import { MailConversationClient } from "@bluemind/backend.mail.api";

registerAPIClients();
store.registerModule("mail", MailStore);
store.registerModule("mail-webapp", MailWebAppStore);

Scheduler.startUnreadCountersUpdater();

router.addRoutes(mailRoutes);

Vue.component("mail-webapp", MailApp);
Vue.component("MailAlertRenderer", MailAlertRenderer);
for (let component in AlertComponents) {
    Vue.component(component, AlertComponents[component]);
}

function registerAPIClients() {
    injector.register({
        provide: "MailboxFoldersPersistence",
        factory: mailboxUid => {
            const userSession = injector.getProvider("UserSession").get();
            return new MailboxFoldersClientProxy(userSession.sid, userSession.domain.replace(".", "_"), mailboxUid);
        }
    });

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
        provide: "SubscriptionPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new OwnerSubscriptionsClient(userSession.sid, userSession.domain, userSession.userId);
        }
    });

    injector.register({
        provide: "ContainersPersistence",
        factory: () => new ContainersClient(injector.getProvider("UserSession").get().sid)
    });

    injector.register({
        provide: "AddressBooksPersistence",
        factory: () => new AddressBooksClient(injector.getProvider("UserSession").get().sid)
    });

    injector.register({
        provide: "TaskService",
        factory: taskId => new TaskClient(injector.getProvider("UserSession").get().sid, taskId)
    });

    injector.register({
        provide: "MailboxesPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new MailboxesClient(userSession.sid, userSession.domain);
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
        provide: "CalendarPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new CalendarClient(userSession.sid, "calendar:Default:" + userSession.userId);
        }
    });

    injector.register({
        provide: "IUserMailIdentities",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new UserMailIdentitiesClient(userSession.sid, userSession.domain, userSession.userId);
        }
    });

    injector.register({
        provide: "MailConversationPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            const conversationContainerId =
                "subtree_" +
                userSession.domain.replace(".", "_") +
                "!user." +
                userSession.login.split("@")[0] +
                "_conversations";
            return new MailConversationClient(userSession.sid, conversationContainerId);
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
            console.log("Registration succeeded. Scope is " + registration.scope);
        } catch (error) {
            console.log("Registration failed with " + error);
        }

        navigator.serviceWorker.addEventListener("message", event => {
            if (event.data.type === "ERROR") {
                showNotification(event.data.payload.message);
            }
        });

        navigator.serviceWorker.addEventListener("waiting", () => {
            console.warn(
                "A new service worker has installed, but it can't activate until all tabs running the current version have fully unloaded."
            );
        });

        navigator.serviceWorker.addEventListener("installed", event => {
            if (event.isUpdate) {
                showNotification("A new version of the site is available, please refresh the page.");
            }
        });
        if (navigator.serviceWorker.controller) {
            navigator.serviceWorker.controller.postMessage({ type: "INIT_PERIODIC_SYNC" });
        }
    }
})();
