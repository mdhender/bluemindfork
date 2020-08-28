import { purge as purgeAction } from "../../actions/purgeAndRemove";
import ItemUri from "@bluemind/item-uri";

let isMessageRemoveActionSuccessfull = true;
let mockMessage;
const messageKey = ItemUri.encode("message-id", "trash-key");
const context = {
    dispatch: jest.fn().mockImplementation(arg => {
        if (arg === "$_getIfNotPresent") {
            return Promise.resolve([mockMessage]);
        } else if (arg === "messages/remove") {
            return isMessageRemoveActionSuccessfull ? Promise.resolve({}) : Promise.reject();
        }
    }),
    commit: jest.fn(),
    state: {
        currentMessage: { key: messageKey }
    },
    rootGetters: {
        "mail/MY_TRASH": { key: "trash-key" }
    },
    rootState: {
        mail: {
            folders: { "trash-key": { key: "trash-key", unread: 10 } }
        }
    }
};

describe("MailApp Store: Purge message action", () => {
    beforeEach(() => {
        isMessageRemoveActionSuccessfull = true;
        mockMessage = { subject: undefined, states: "not-a-valid-state", key: messageKey };
        context.dispatch.mockClear();
        context.commit.mockClear();
    });

    test("dispatch right actions and mutate alerts state when action is successful", async () => {
        await purgeAction(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [messageKey]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/remove", [messageKey]);

        expect(context.commit).toHaveBeenCalledTimes(2);

        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_PURGE_OK",
                props: { subject: undefined }
            }),
            { root: true }
        );
    });

    test("if purge action fails, an error alert mutation is emitted", async () => {
        isMessageRemoveActionSuccessfull = false;

        await purgeAction(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [messageKey]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/remove", [messageKey]);

        expect(context.commit).toHaveBeenCalledTimes(1);
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_PURGE_ERROR",
                props: { subject: undefined, reason: undefined }
            }),
            { root: true }
        );
    });

    test("update the unread counter if necessary", async () => {
        await purgeAction(context, messageKey);
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "loadUnreadCount", "trash-key");
    });
});
