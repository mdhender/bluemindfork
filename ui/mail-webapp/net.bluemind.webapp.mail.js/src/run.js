import { AddressBooksClient } from "@bluemind/addressbook.api";
import { MailboxFoldersClient, MailboxItemsClient, OutboxClient } from "@bluemind/backend.mail.api";
import { MailboxFoldersStore, MailboxItemsStore, OutboxStore } from "@bluemind/backend.mail.store";
import { TaskClient } from "@bluemind/core.task.api";
import AlertStore from "@bluemind/alert.store";
import injector from "@bluemind/inject";
import MailApp from "@bluemind/webapp.mail.ui.vuejs";
import mailRoutes from "./router";
import router from "@bluemind/router";
import store from "@bluemind/store";
import Vue from "vue";

registerAPIClients();
registerStores();

router.addRoutes(mailRoutes);
Vue.component("mail-webapp", MailApp);


function registerStores() {
    store.registerModule("backend.mail/folders", MailboxFoldersStore);
    store.registerModule("backend.mail/items", MailboxItemsStore);
    store.registerModule("backend.mail/outbox", OutboxStore);
    store.registerModule("alert", AlertStore);
}

function registerAPIClients() {
    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "MailboxFoldersPersistance",
        factory: () => {
            const userSession = injector.getProvider('UserSession').get();
            return new MailboxFoldersClient(
                userSession.sid,
                userSession.domain.replace(".", "_"),
                "user." + userSession.login.split("@")[0]
            );
        }
    });
    
    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "MailboxItemsPersistance",
        factory: uid => new MailboxItemsClient(injector.getProvider('UserSession').get().sid, uid)
    });
    
    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "OutboxPersistance",
        factory: () => {
            const userSession = injector.getProvider('UserSession').get();
            return new OutboxClient(
                userSession.sid,
                userSession.domain,
                userSession.userId
            );
        }
    });
        
    injector.register({
        // FIXME in fact it is not the persistence layer, use XxxAPI or XxxService instead
        provide: "AddressBooksPersistance",
        factory: () => new AddressBooksClient(injector.getProvider('UserSession').get().sid)
    });

    injector.register({
        provide: "TaskService",
        factory: taskId => new TaskClient(injector.getProvider('UserSession').get().sid, taskId)
    });
}
