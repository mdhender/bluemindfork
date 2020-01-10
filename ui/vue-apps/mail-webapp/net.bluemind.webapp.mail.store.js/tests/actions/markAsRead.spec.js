import { markAsRead } from "../../src/actions/markAsRead";
import ItemUri from "@bluemind/item-uri";

const messageId = "74515",
    folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23";
const messageKey = ItemUri.encode(messageId, folderUid);
const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve({ states: ["not-seen"] })),
    state: {
        foldersData: {
            [folderUid]: {
                unread: 10
            }
        }
    }
};

describe("[Mail-WebappStore][actions] : markAsRead", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });

    test("call update seen for a given message and mutate state", done => {
        markAsRead(context, messageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", messageKey);
            expect(context.dispatch).toHaveBeenCalledWith("messages/updateSeen", { messageKey, isSeen: true });
            expect(context.commit).toHaveBeenCalledWith("setUnreadCount", { folderUid, count: 9 });
            done();
        });
    });

    test("call update seen only if message is unseen", done => {
        context.dispatch.mockReturnValueOnce(Promise.resolve({ states: ["seen"] }));
        markAsRead(context, messageKey).then(() => {
            expect(context.commit).not.toHaveBeenCalled();
            expect(context.dispatch).not.toHaveBeenCalledWith("folder/updateSeen", { messageKey, isSeen: true });
            done();
        });
    });
});
