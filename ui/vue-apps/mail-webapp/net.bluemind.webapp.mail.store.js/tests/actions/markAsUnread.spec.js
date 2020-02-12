import { Flag } from "@bluemind/email";
import { markAsUnread } from "../../src/actions/markAsUnread";
import ItemUri from "@bluemind/item-uri";

const messageId = "74515",
    folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23";
const messageKey = ItemUri.encode(messageId, folderUid);
const mailboxItemFlag = Flag.SEEN;
const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve({ states: ["seen"] })),
    state: {
        foldersData: {
            [folderUid]: {
                unread: 10
            }
        }
    }
};

describe("[Mail-WebappStore][actions] : markAsUnead", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });

    test("call update seen for a given message and mutate state", done => {
        markAsUnread(context, messageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", messageKey);
            expect(context.dispatch).toHaveBeenCalledWith("messages/deleteFlag", { messageKey, mailboxItemFlag });
            expect(context.commit).toHaveBeenCalledWith("setUnreadCount", { folderUid, count: 11 });
            done();
        });
    });

    test("call update seen only if message is seen", done => {
        context.dispatch.mockReturnValueOnce(Promise.resolve({ states: ["not-seen"] }));
        markAsUnread(context, messageKey).then(() => {
            expect(context.commit).not.toHaveBeenCalled();
            expect(context.dispatch).not.toHaveBeenCalledWith("folder/deleteFlag", { messageKey, mailboxItemFlag });
            done();
        });
    });
});
