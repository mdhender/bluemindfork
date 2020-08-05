import { remove } from "../../src/actions/purgeAndRemove";
import ItemUri from "@bluemind/item-uri";

const messageKey = ItemUri.encode("message-id", "source-uid");
let mockMessage;

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockImplementation(arg => {
        if (arg === "$_getIfNotPresent") {
            return Promise.resolve([mockMessage]);
        }
    }),
    getters: {
        my: {
            mailboxUid: "mailbox-uid",
            TRASH: { key: "trash-key", uid: "trash-uid" },
            INBOX: { key: "inbox-key", uid: "inbox-uid" }
        },
        "folders/getFolderByKey": jest.fn().mockReturnValue({
            internalId: 10
        })
    },
    state: {
        foldersData: {
            ["inbox-uid"]: {
                unread: 10
            }
        },
        currentMessage: { key: messageKey }
    }
};

describe("[Mail-WebappStore][actions] : remove", () => {
    beforeEach(() => {
        mockMessage = { subject: "dummy", states: "not-a-valid-state", key: messageKey };
        context.commit.mockClear();
        context.dispatch.mockClear();
    });
    test("call private move action", async () => {
        await remove(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("$_move", {
            messageKeys: [messageKey],
            destinationKey: "trash-key"
        });

        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [messageKey]);
    });
    test("display alerts", async () => {
        await remove(context, messageKey);
        expect(context.commit).toHaveBeenNthCalledWith(
            1,
            "addApplicationAlert",
            {
                code: "MSG_REMOVED_LOADING",
                props: { subject: "dummy" },
                uid: expect.anything()
            },
            { root: true }
        );

        expect(context.commit).toHaveBeenNthCalledWith(
            3,
            "addApplicationAlert",
            {
                code: "MSG_REMOVED_OK",
                props: { subject: "dummy" },
                uid: undefined
            },
            { root: true }
        );
        expect(context.commit).toHaveBeenNthCalledWith(4, "removeApplicationAlert", expect.anything(), {
            root: true
        });
    });
    test("remove message definitely if current folder is the trash", async () => {
        const messageKey = ItemUri.encode("message-id", "trash-uid");
        await remove(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("messages/remove", [messageKey]);
    });
    test("update the unread counter if necessary", async () => {
        const messageKey = ItemUri.encode("message-id", "inbox-uid");

        // call remove without any unread mail, do not expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state hello-there", key: messageKey };
        await remove(context, messageKey);

        expect(context.commit).not.toHaveBeenCalledWith("setUnreadCount");

        // call remove with unread mails, expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state not-seen", key: messageKey };
        await remove(context, messageKey);

        expect(context.commit).toHaveBeenCalledWith("setUnreadCount", { folderUid: "inbox-uid", count: 9 });
    });
});
