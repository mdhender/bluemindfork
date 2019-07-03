import Vue from "vue";
import MailApp from "@bluemind/webapp.mail.ui.vuejs";
import injector from "@bluemind/inject";
import store from "@bluemind/store";
import { MailboxFoldersClient, MailboxItemsClient, OutboxClient } from "@bluemind/backend.mail.api";
import { MailboxFoldersStore, MailboxItemsStore, OutboxStore } from "@bluemind/backend.mail.store";
import AlertStore from "@bluemind/alert.store";
import mailRoutes from "./router";
import router from "@bluemind/router";

injector.register({
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
    provide: "MailboxItemsPersistance",
    factory: uid => new MailboxItemsClient(injector.getProvider('UserSession').get().sid, uid)
});
injector.register({
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

store.registerModule("backend.mail/folders", MailboxFoldersStore);
store.registerModule("backend.mail/items", MailboxItemsStore);
store.registerModule("backend.mail/outbox", OutboxStore);
store.registerModule("alert", AlertStore);

router.addRoutes(mailRoutes);

Vue.component("mail-webapp", MailApp);
