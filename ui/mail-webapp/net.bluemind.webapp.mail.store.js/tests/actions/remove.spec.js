import { remove } from "../../src/actions/remove";
import ItemUri from "@bluemind/item-uri";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve({ subject: "dummy" })),
    getters: {
        my: { mailboxUid: "mailbox-uid", TRASH: { key: "trash-key", uid: "trash-uid" } },
        "folders/getFolderByKey": jest.fn().mockReturnValue({
            internalId: 10
        })
    }
};

describe("[Mail-WebappStore][actions] : remove", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });
    test("call private move action", done => {
        const messageKey = ItemUri.encode("message-id", "source-uid");
        remove(context, messageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_move", { messageKey, destinationKey: "trash-key" });
            done();
        });
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", messageKey);
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
});
