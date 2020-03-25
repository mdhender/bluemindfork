import { remove } from "../../src/actions/remove";
import ItemUri from "@bluemind/item-uri";

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
        }
    }
};

describe("[Mail-WebappStore][actions] : remove", () => {
    beforeEach(() => {
        mockMessage = { subject: "dummy", states: "not-a-valid-state" };
        context.commit.mockClear();
        context.dispatch.mockClear();
    });
    test("call private move action", done => {
        const messageKey = ItemUri.encode("message-id", "source-uid");
        remove(context, messageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_move", {
                messageKeys: [messageKey],
                destinationKey: "trash-key"
            });
            done();
        });
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [messageKey]);
    });
    test("display alerts", done => {
        const messageKey = ItemUri.encode("message-id", "source-uid");
        remove(context, messageKey).then(() => {
            expect(context.commit).toHaveBeenNthCalledWith(
                1,
                "alert/add",
                {
                    code: "MSG_REMOVED_LOADING",
                    props: { subject: "dummy" },
                    uid: expect.anything()
                },
                { root: true }
            );

            expect(context.commit).toHaveBeenNthCalledWith(
                2,
                "alert/add",
                {
                    code: "MSG_REMOVED_OK",
                    props: { subject: "dummy" }
                },
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(3, "alert/remove", expect.anything(), { root: true });
            done();
        });
    });
    test("remove message definitely if current folder is the trash", () => {
        const messageKey = ItemUri.encode("message-id", "trash-uid");
        remove(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("purge", messageKey);
    });
    test("update the unread counter if necessary", done => {
        const messageKey = ItemUri.encode("message-id", "inbox-uid");

        // call remove without any unread mail, do not expect to update the unread counter
        mockMessage = { subject: "dummy", states: "not-a-valid-state hello-there" };
        remove(context, messageKey)
            .then(() => {
                expect(context.commit).not.toHaveBeenCalledWith("setUnreadCount");

                // call remove with unread mails, expect to update the unread counter
                mockMessage = { subject: "dummy", states: "not-a-valid-state not-seen" };
                return remove(context, messageKey);
            })
            .then(() => {
                expect(context.commit).toHaveBeenCalledWith("setUnreadCount", { folderUid: "inbox-uid", count: 9 });
                done();
            });
    });
});
