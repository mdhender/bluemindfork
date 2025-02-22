import {
    ItemsTransferClient,
    MailboxItemsClient,
    MailConversationActionsClient,
    MailConversationClient,
    OutboxClient
} from "@bluemind/backend.mail.api";
import injector, { inject } from "@bluemind/inject";
import { MailTipClient } from "@bluemind/mailmessage.api";

export default function () {
    injector.register({
        provide: "MailboxItemsPersistence",
        factory: uid => new MailboxItemsClient(inject("UserSession").sid, uid)
    });

    injector.register({
        provide: "OutboxPersistence",
        factory: () => {
            const userSession = inject("UserSession");
            return new OutboxClient(userSession.sid, userSession.domain, userSession.userId);
        }
    });

    injector.register({
        provide: "ItemsTransferPersistence",
        factory: (sourceUid, destinationUid) => {
            const userSession = inject("UserSession");
            return new ItemsTransferClient(userSession.sid, sourceUid, destinationUid);
        }
    });

    injector.register({
        provide: "MailConversationActionsPersistence",
        factory: (mailboxUid, folderUid) => {
            const userSession = inject("UserSession");
            const conversationContainerId = "subtree_" + userSession.domain.replaceAll(".", "_") + "!" + mailboxUid;
            return new MailConversationActionsClient(userSession.sid, conversationContainerId, folderUid);
        }
    });

    injector.register({
        provide: "MailConversationPersistence",
        factory: mailboxUid => {
            const userSession = inject("UserSession");
            const conversationContainerId = "subtree_" + userSession.domain.replaceAll(".", "_") + "!" + mailboxUid;
            return new MailConversationClient(userSession.sid, conversationContainerId);
        }
    });

    injector.register({
        provide: "MailTipPersistence",
        factory: () => {
            const userSession = inject("UserSession");
            return new MailTipClient(userSession.sid, userSession.domain);
        }
    });
}
