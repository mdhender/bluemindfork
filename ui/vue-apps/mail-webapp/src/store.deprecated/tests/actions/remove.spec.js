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
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            {
                code: "MSG_REMOVED_OK",
                props: { subject: "dummy" },
                uid: undefined
            },
            { root: true }
        );
    });

    test("remove message definitely if current folder is the trash", async () => {
        const messageKey = ItemUri.encode("message-id", "trash-key");
        await remove(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("messages/remove", [messageKey]);
    });

    test("update the unread counter if necessary", async () => {
        const messageKey = ItemUri.encode("message-id", "inbox-uid");
        await remove(context, messageKey);
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "mail-webapp/loadUnreadCount", "inbox-uid");
    });
});
