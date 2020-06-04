import { AddressBooksClient } from "@bluemind/addressbook.api";
import { AlertFactory } from "@bluemind/alert.store";
import { CalendarClient } from "@bluemind/calendar.api";
import { ContainersClient } from "@bluemind/core.container.api";
import { ItemsTransferClient } from "@bluemind/backend.mail.api";
import { MailboxesClient } from "@bluemind/mailbox.api";
import { MailboxFoldersClient, OutboxClient } from "@bluemind/backend.mail.api";
import { MailboxItemsService } from "@bluemind/backend.mail.service";
import { TaskClient } from "@bluemind/core.task.api";
import { UserSettingsClient } from "@bluemind/user.api";
import injector from "@bluemind/inject";
import { MailAlertRenderer } from "@bluemind/webapp.mail.ui.vuejs";
import MailApp from "@bluemind/webapp.mail.ui.vuejs";
import MailAppAlerts from "@bluemind/webapp.mail.alert";
import mailRoutes from "./router";
import MailWebAppStore from "@bluemind/webapp.mail.store.deprecated";
import MailStore from "@bluemind/webapp.mail.store";
import router from "@bluemind/router";
import store from "@bluemind/store";
import Vue from "vue";
import Scheduler from "./scheduler";

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
