import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { MESSAGE_IS_LOADED } from "~getters";
import { SET_UNREAD_COUNT } from "~mutations";
import { markAsUnread } from "../../actions/markAs";

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
            folders: {
                [folderUid]: {
                    key: folderUid,
                    unread: 10
                },
                [folderUid2]: {
                    key: folderUid2,
                    unread: 1
                }
            },
            messages: {
                [messageKey1]: { key: messageKey1, flags: [] },
                [messageKey2]: { key: messageKey2, flags: [] },
                [messageKey3]: { key: messageKey3, flags: [Flag.SEEN] },
                [messageKey4]: { key: messageKey4, flags: [] },
                [messageKeyOtherFolder]: { key: messageKeyOtherFolder, flags: [] }
            }
        }
    },
    rootGetters: {
        ["mail/" + MESSAGE_IS_LOADED]: key => [messageKey1, messageKey3].includes(key)
    }
};

describe("[Mail-WebappStore][actions] : markAsUnead", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });

    test("call update seen for a given message and mutate state (message is in state)", async () => {
        await markAsUnread(context, [messageKey3]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/deleteFlag", {
            messageKeys: [messageKey3],
            mailboxItemFlag
        });
        expect(context.commit).toHaveBeenCalledWith(
            "mail/" + SET_UNREAD_COUNT,
            { key: folderUid, count: 11 },
            { root: true }
        );
    });

    test("call update seen only if message is seen (message is in state)", async () => {
        await markAsUnread(context, [messageKey1]);
        expect(context.commit).not.toHaveBeenCalled();
        expect(context.dispatch).not.toHaveBeenCalledWith("messages/deleteFlag", {
            messageKeys: [messageKey1],
            mailboxItemFlag
        });
    });

    test("Multiple messages, same folder (missing messages)", async () => {
        const messageKeys = [messageKey1, messageKey2, messageKey3, messageKey4];
        await markAsUnread(context, messageKeys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/deleteFlag", {
            messageKeys: [messageKey1, messageKey2, messageKey3, messageKey4],
            mailboxItemFlag
        });
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid);
    });

    test("Multiple messages, same folder (messages are in state)", async () => {
        const messageKeys = [messageKey1, messageKey3];
        await markAsUnread(context, messageKeys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/deleteFlag", {
            messageKeys: [messageKey3],
            mailboxItemFlag
        });
        expect(context.commit).toHaveBeenCalledWith(
            "mail/" + SET_UNREAD_COUNT,
            { key: folderUid, count: 11 },
            { root: true }
        );
    });

    test("Multiple messages, different folders (missing messages)", async () => {
        const messageKeys = [messageKey1, messageKey2, messageKey3, messageKey4, messageKeyOtherFolder];
        await markAsUnread(context, messageKeys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/deleteFlag", {
            messageKeys,
            mailboxItemFlag
        });
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid);
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid2);
    });
});
