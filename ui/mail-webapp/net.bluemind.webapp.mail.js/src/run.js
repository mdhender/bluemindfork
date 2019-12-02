import { AddressBooksClient } from "@bluemind/addressbook.api";
import { AlertFactory } from "@bluemind/alert.store";
import { MailboxFoldersClient, MailboxItemsClient, OutboxClient } from "@bluemind/backend.mail.api";
import { TaskClient } from "@bluemind/core.task.api";
import MailWebAppStore from "@bluemind/webapp.mail.store";
import AlertStore from "@bluemind/alert.store";
import injector from "@bluemind/inject";
import MailApp from "@bluemind/webapp.mail.ui.vuejs";
import MailAppAlerts from "@bluemind/webapp.mail.alert";
import mailRoutes from "./router";
import router from "@bluemind/router";
import store from "@bluemind/store";
import Vue from "vue";

registerAPIClients();
registerStores();

AlertFactory.register(MailAppAlerts);
router.addRoutes(mailRoutes);

Vue.component("mail-webapp", MailApp);

function registerStores() {
    store.registerModule("mail-webapp", MailWebAppStore);
    store.registerModule("alert", AlertStore);
}

function registerAPIClients() {
    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "MailboxFoldersPersistence",
        factory: mailboxUid => {
            const userSession = injector.getProvider("UserSession").get();
            return new MailboxFoldersClient(
                userSession.sid,
                userSession.domain.replace(".", "_"),
                mailboxUid
                // "user." + userSession.login.split("@")[0]
            );
        }
    });

    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "MailboxItemsPersistence",
        factory: uid => new MailboxItemsClient(injector.getProvider("UserSession").get().sid, uid)
    });

    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "OutboxPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new OutboxClient(userSession.sid, userSession.domain, userSession.userId);
        }
    });

    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "AddressBooksPersistence",
        factory: () => new AddressBooksClient(injector.getProvider("UserSession").get().sid)
    });

    injector.register({
        provide: "TaskService",
        factory: taskId => new TaskClient(injector.getProvider("UserSession").get().sid, taskId)
    });
}
