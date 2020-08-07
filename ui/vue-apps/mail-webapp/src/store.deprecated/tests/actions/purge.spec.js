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
    const expectedLoadingAlert = {
        code: "MSG_PURGE_LOADING",
        props: { subject: undefined }
    };

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

        expect(context.commit).toHaveBeenCalledTimes(4);
        let loadingAlertUid = context.commit.mock.calls[0][1].uid;

        expect(context.commit).toHaveBeenNthCalledWith(
            1,
            "addApplicationAlert",
            expect.objectContaining(expectedLoadingAlert),
            { root: true }
        );

        expect(context.commit).toHaveBeenNthCalledWith(4, "removeApplicationAlert", loadingAlertUid, {
            root: true
        });

        expect(context.commit).toHaveBeenNthCalledWith(
            3,
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

        expect(context.commit).toHaveBeenCalledTimes(4);
        let loadingAlertUid = context.commit.mock.calls[0][1].uid;

        expect(context.commit).toHaveBeenNthCalledWith(
            1,
            "addApplicationAlert",
            expect.objectContaining(expectedLoadingAlert),
            { root: true }
        );
        expect(context.commit).toHaveBeenNthCalledWith(4, "removeApplicationAlert", loadingAlertUid, {
            root: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(
            3,
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_PURGE_ERROR",
                props: { subject: undefined, reason: undefined }
            }),
            { root: true }
        );
    });

    test("update the unread counter if necessary", async () => {
        // call remove without any unread mail, do not expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state hello-there", key: messageKey };
        await purgeAction(context, messageKey);

        expect(context.commit).not.toHaveBeenCalledWith("mail/SET_UNREAD_COUNT");

        // call remove with unread mails, expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state not-seen", key: messageKey };
        await purgeAction(context, messageKey);

        expect(context.commit).toHaveBeenNthCalledWith(
            6,
            "mail/SET_UNREAD_COUNT",
            { key: "trash-key", count: 9 },
            { root: true }
        );
    });
});
