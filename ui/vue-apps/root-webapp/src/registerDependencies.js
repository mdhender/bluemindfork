import {
    AddressBookClient,
    AddressBooksClient,
    AddressBooksMgmtClient,
    VCardServiceClient
} from "@bluemind/addressbook.api";
import { UserAnnouncementsClient } from "@bluemind/announcement.api";
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
import { TagsClient } from "@bluemind/tag.api";
import { TodoListClient, TodoListsClient, VTodoClient } from "@bluemind/todolist.api";
import {
    UserClient,
    UserMailIdentitiesClient,
    UserSubscriptionClient,
    UserExternalAccountClient
} from "@bluemind/user.api";
import { ExternalSystemClient } from "@bluemind/system.api";
import VueBus from "@bluemind/vue-bus";
import { MailboxesClientProxy, MailboxFoldersClientProxy, UserSettingsClientProxy } from "./apiProxies";

export default function (userSession) {
    injector.register({
        provide: "AddressBooksMgmtPersistence",
        factory: () => new AddressBooksMgmtClient(userSession.sid)
    });

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
        provide: "ExternalSystemPersistence",
        factory: () => new ExternalSystemClient(userSession.sid)
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
        factory: () => new MailboxesClientProxy(userSession.sid, userSession.domain)
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
        provide: "TagsPersistence",
        factory: containerUid => new TagsClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "TaskService",
        factory: taskId => new TaskClient(userSession.sid, taskId)
    });

    injector.register({
        provide: "MailboxFoldersPersistence",
        factory: mailboxUid => {
            return new MailboxFoldersClientProxy(userSession.sid, userSession.domain.replace(".", "_"), mailboxUid);
        }
    });

    injector.register({
        provide: "TodoListPersistence",
        factory: taskId => new TodoListClient(userSession.sid, taskId)
    });

    injector.register({
        provide: "TodoListsPersistence",
        factory: taskId => new TodoListsClient(userSession.sid, taskId)
    });

    injector.register({
        provide: "UserAnnouncementsPersistence",
        factory: () => new UserAnnouncementsClient(userSession.sid, userSession.userId)
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
        provide: "UserExternalAccountPersistence",
        factory: () => new UserExternalAccountClient(userSession.sid, userSession.domain, userSession.userId)
    });

    injector.register({
        provide: "UserPersistence",
        factory: () => new UserClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "UserSettingsPersistence",
        factory: () => new UserSettingsClientProxy(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "VCardServicePersistence",
        factory: containerUid => new VCardServiceClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "VEventPersistence",
        factory: containerUid => new VEventClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "VTodoPersistence",
        factory: containerUid => new VTodoClient(userSession.sid, containerUid)
    });
}
