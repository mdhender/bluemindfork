import { AddressBooksClient } from "@bluemind/addressbook.api";
import { AlertFactory } from "@bluemind/alert.store";
import { MailboxFoldersClient, OutboxClient } from "@bluemind/backend.mail.api";
import { MailboxItemsService } from "@bluemind/backend.mail.service";
import { TaskClient } from "@bluemind/core.task.api";
import { UserSettingsClient } from "@bluemind/user.api";
import MailWebAppStore from "@bluemind/webapp.mail.store";
import { ContainersClient } from "@bluemind/core.container.api";
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
        provide: "MailboxFoldersPersistence",
        factory: mailboxUid => {
            const userSession = injector.getProvider("UserSession").get();
            return new MailboxFoldersClient(userSession.sid, userSession.domain.replace(".", "_"), mailboxUid);
        }
    });

    injector.register({
        provide: "MailboxItemsPersistence",
        factory: uid => new MailboxItemsService(injector.getProvider("UserSession").get().sid, uid)
    });

    injector.register({
        provide: "OutboxPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new OutboxClient(userSession.sid, userSession.domain, userSession.userId);
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
        provide: "UserSettingsPersistence",
        factory: () => {
            const userSession = injector.getProvider("UserSession").get();
            return new UserSettingsClient(userSession.sid, userSession.domain);
        }
    });
}
