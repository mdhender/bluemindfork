import { AddressBookClient, AddressBooksClient } from "@bluemind/addressbook.api";
import { APIKeysClient } from "@bluemind/authentication.api";
import {
    CalendarClient,
    CalendarsMgmtClient,
    FreebusyMgmtClient,
    PublishCalendarClient,
    VEventClient
} from "@bluemind/calendar.api";
import {
    ContainersClient,
    ContainerManagementClient,
    ContainerSyncClient,
    OwnerSubscriptionsClient
} from "@bluemind/core.container.api";
import { TaskClient } from "@bluemind/core.task.api";
import { DirectoryClient } from "@bluemind/directory.api";
import { FirstDayOfWeek } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import { MailboxesClient } from "@bluemind/mailbox.api";
import { UserClient, UserMailIdentitiesClient, UserSettingsClient, UserSubscriptionClient } from "@bluemind/user.api";
import VueBus from "@bluemind/vue-bus";

export default function (userSession) {
    injector.register({
        provide: "AddressBooksPersistence",
        factory: () => new AddressBooksClient(userSession.sid)
    });

    injector.register({
        provide: "AddressBookPersistence",
        factory: containerUid => new AddressBookClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "APIKeysPersistence",
        factory: () => new APIKeysClient(userSession.sid)
    });

    injector.register({
        provide: "CalendarsMgmtPersistence",
        factory: () => new CalendarsMgmtClient(userSession.sid)
    });

    injector.register({
        provide: "CalendarPersistence",
        factory: containerUid => {
            if (!containerUid) {
                return new CalendarClient(userSession.sid, "calendar:Default:" + userSession.userId);
            }
            return new CalendarClient(userSession.sid, containerUid);
        }
    });

    injector.register({
        provide: "ContainersPersistence",
        factory: () => new ContainersClient(userSession.sid)
    });

    injector.register({
        provide: "ContainerManagementPersistence",
        factory: containerUid => new ContainerManagementClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "ContainerSyncPersistence",
        factory: containerUid => new ContainerSyncClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "DirectoryPersistence",
        factory: () => new DirectoryClient(userSession.sid, userSession.domain)
    });

    // if no lang defined, use monday as fdow
    let firstDayOfWeek = FirstDayOfWeek[userSession.lang.toUpperCase()];
    firstDayOfWeek = firstDayOfWeek >= 0 ? firstDayOfWeek : 1;

    injector.register({
        provide: "Environment",
        use: { firstDayOfWeek }
    });

    injector.register({
        provide: "FreebusyMgmtPersistence",
        factory: containerUid => new FreebusyMgmtClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "GlobalEventBus",
        use: VueBus.Client
    });

    injector.register({
        provide: "MailboxesPersistence",
        factory: () => new MailboxesClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "OwnerSubscriptionsPersistence",
        factory: () => new OwnerSubscriptionsClient(userSession.sid, userSession.domain, userSession.userId)
    });

    injector.register({
        provide: "PublishCalendarPersistence",
        factory: containerUid => new PublishCalendarClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "TaskService",
        factory: taskId => new TaskClient(userSession.sid, taskId)
    });

    injector.register({
        provide: "UserSubscriptionPersistence",
        factory: () => new UserSubscriptionClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "UserMailIdentitiesPersistence",
        factory: () => new UserMailIdentitiesClient(userSession.sid, userSession.domain, userSession.userId)
    });

    injector.register({
        provide: "UserPersistence",
        factory: () => new UserClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "UserSettingsPersistence",
        factory: () => new UserSettingsClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "VEventPersistence",
        factory: containerUid => new VEventClient(userSession.sid, containerUid)
    });
}
