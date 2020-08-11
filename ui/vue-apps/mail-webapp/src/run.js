import Vue from "vue";
import { AddressBooksClient } from "@bluemind/addressbook.api";
import { AlertFactory } from "@bluemind/alert.store";
import { CalendarClient } from "@bluemind/calendar.api";
import { ContainersClient } from "@bluemind/core.container.api";
import { ItemsTransferClient } from "@bluemind/backend.mail.api";
import { MailboxesClient } from "@bluemind/mailbox.api";
import { OutboxClient } from "@bluemind/backend.mail.api";
import { OwnerSubscriptionsClient } from "@bluemind/core.container.api";
import { TaskClient } from "@bluemind/core.task.api";
import injector from "@bluemind/inject";
import global from "@bluemind/global";
import router from "@bluemind/router";
import store from "@bluemind/store";
import MailAlertRenderer from "./components/MailAlertRenderer";
import MailApp from "./components/MailApp";
import MailAppAlerts from "./alerts";
import { MailboxItemsClientProxy, MailboxFoldersClientProxy } from "./store.deprecated/mailbackend/APIClientsProxy";
import mailRoutes from "./router";
import Scheduler from "./scheduler";
import MailStore from "./store/";
import MailWebAppStore from "./store.deprecated/";

registerAPIClients();
store.registerModule("mail", MailStore);
store.registerModule("mail-webapp", MailWebAppStore);

Scheduler.startUnreadCountersUpdater();

AlertFactory.register(MailAppAlerts);
router.addRoutes(mailRoutes);

Vue.component("mail-webapp", MailApp);
Vue.component("MailAlertRenderer", MailAlertRenderer);

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
}

global.activateServiceWorker = process.env.NODE_ENV === "development";

if ("serviceWorker" in navigator && global.activateServiceWorker) {
    window.addEventListener("load", () => {
        navigator.serviceWorker
            .register("service-worker.js")
            .then(registration => {
                console.log("Service Worker registered. ", registration);
            })
            .catch(error => {
                console.error("Service Worker registered failed. ", error);
            });
    });
}
