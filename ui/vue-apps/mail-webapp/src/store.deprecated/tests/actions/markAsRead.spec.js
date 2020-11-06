import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { markAsRead } from "../../actions/markAs";

const messageId = "74515";
const folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23";
const folderUid2 = "11111111-8c78-4cc3-baf0-1ae3dfe24a23";
const messageKey1 = ItemUri.encode(messageId + "1", folderUid);
const messageKey2 = ItemUri.encode(messageId + "2", folderUid);
const messageKey3 = ItemUri.encode(messageId + "3", folderUid);
const messageKey4 = ItemUri.encode(messageId + "4", folderUid);
const messageKeyOtherFolder = ItemUri.encode(messageId + "9", folderUid2);
const mailboxItemFlag = Flag.SEEN;
const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve([{ flags: [], key: messageKey1 }])),
    rootState: {
        mail: {
            messages: {
                [messageKey1]: { key: messageKey1, flags: [] },
                [messageKey2]: { key: messageKey2, flags: [] },
                [messageKey3]: { key: messageKey3, flags: [Flag.SEEN] },
                [messageKey4]: { key: messageKey4, flags: [] },
                [messageKeyOtherFolder]: { key: messageKeyOtherFolder, flags: [] }
            },
            folders: {
                [folderUid]: {
                    key: folderUid,
                    unread: 10
                },
                [folderUid2]: {
                    key: folderUid2,
                    unread: 1
                }
            }
        }
    },
    rootGetters: {
        "mail/isLoaded": key => [messageKey1, messageKey3].includes(key)
    }
};

describe("[Mail-WebappStore][actions] : markAsRead", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });

    test("call update seen for a given message and mutate state (no missing messages)", async () => {
        await markAsRead(context, [messageKey1]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/addFlag", {
            messageKeys: [messageKey1],
            mailboxItemFlag
        });
        expect(context.commit).toHaveBeenCalledWith(
            "mail/SET_UNREAD_COUNT",
            { key: folderUid, count: 9 },
            { root: true }
        );
        checkAlertsHaveNotBeenCalled();
    });

    test("call update seen for a given message and mutate state (missing messages)", async () => {
        await markAsRead(context, [messageKey2]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/addFlag", {
            messageKeys: [messageKey2],
            mailboxItemFlag
        });
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid);
        checkAlertsHaveNotBeenCalled();
    });

    test("call update seen only if message is unseen or not in state", async () => {
        await markAsRead(context, [messageKey3]);
        expect(context.commit).not.toHaveBeenCalled();
        expect(context.dispatch).not.toHaveBeenCalledWith("messages/addFlag", {
            messageKeys: [messageKey3],
            mailboxItemFlag
        });
        checkAlertsHaveNotBeenCalled();
    });

    test("Multiple messages, same folder (missing messages)", async () => {
        const messageKeys = [messageKey1, messageKey2, messageKey3, messageKey4];
        await markAsRead(context, messageKeys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/addFlag", {
            messageKeys,
            mailboxItemFlag
        });
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid);
        checkAlertsHaveBeenCalled();
    });

    test("Multiple messages, different folders", async () => {
        const messageKeys = [messageKey1, messageKey2, messageKey3, messageKey4, messageKeyOtherFolder];
        await markAsRead(context, messageKeys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/addFlag", {
            messageKeys: [messageKey1, messageKey2, messageKey3, messageKey4, messageKeyOtherFolder],
            mailboxItemFlag
        });
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid);
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid2);
        checkAlertsHaveBeenCalled();
    });

    function checkAlertsHaveNotBeenCalled() {
        expect(context.commit).not.toHaveBeenCalledWith("addApplicationAlert", expect.anything(), { root: true });
        expect(context.commit).not.toHaveBeenCalledWith("removeApplicationAlert", expect.anything(), { root: true });
    }

    function checkAlertsHaveBeenCalled() {
        expect(context.commit).toHaveBeenCalledWith("addApplicationAlert", expect.anything(), { root: true });
        expect(context.commit).toHaveBeenCalledWith("removeApplicationAlert", expect.anything(), { root: true });
    }
});
