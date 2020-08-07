import { remove } from "../../actions/purgeAndRemove";
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
    state: {
        currentMessage: { key: messageKey }
    },
    rootGetters: {
        "mail/MY_TRASH": { key: "trash-key" }
    },
    rootState: {
        mail: {
            activeFolder: "trash-key",
            folders: { "inbox-uid": { unread: 10 } }
        }
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
        const messageKey = ItemUri.encode("message-id", "trash-key");
        await remove(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("messages/remove", [messageKey]);
    });

    test("update the unread counter if necessary", async () => {
        const messageKey = ItemUri.encode("message-id", "inbox-uid");

        // call remove without any unread mail, do not expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state hello-there", key: messageKey };
        await remove(context, messageKey);

        expect(context.commit).not.toHaveBeenCalledWith("mail/SET_UNREAD_COUNT");

        // call remove with unread mails, expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state not-seen", key: messageKey };
        await remove(context, messageKey);

        expect(context.commit).toHaveBeenCalledWith(
            "mail/SET_UNREAD_COUNT",
            { key: "inbox-uid", count: 9 },
            { root: true }
        );
    });
});
