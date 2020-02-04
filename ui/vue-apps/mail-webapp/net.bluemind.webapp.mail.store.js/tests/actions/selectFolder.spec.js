import { selectFolder } from "../../src/actions/selectFolder";
import { ItemUri } from "@bluemind/item-uri";
import ContainerObserver from "@bluemind/containerobserver";

jest.mock("@bluemind/containerobserver");

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve([1, 2, 3])),
    commit: jest.fn(),
    state: {
        currentFolderKey: "key",
        messages: { itemKeys: [1, 2, 3] },
        sorted: "up to down",
        messageFilter: null,
        draft: { parts: { attachments: [] } }
    }
};

const folderUid = "folder:uid",
    mailboxUid = "mailbox:uid";
const folderKey = ItemUri.encode(folderUid, mailboxUid);
describe("[Mail-WebappStore][actions] :  selectFolder", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.state.currentFolderKey = folderKey;
        context.state.messages.itemKeys = [1, 2, 3];
        context.state.messageFilter = null;
        ContainerObserver.observe.mockClear();
        ContainerObserver.forget.mockClear();
    });
    test("always clear the current context", () => {
        selectFolder(context, { folderKey });
        expect(context.commit).toHaveBeenCalledWith("setSearchLoading", null);
        expect(context.commit).toHaveBeenCalledWith("setSearchPattern", null);
        expect(context.commit).toHaveBeenCalledWith("currentMessage/clear");
        context.commit.mockClear();
        const another = ItemUri.encode("folderUid", "mailboxUid");
        selectFolder(context, { folderKey: another });
        expect(context.commit).toHaveBeenCalledWith("setSearchLoading", null);
        expect(context.commit).toHaveBeenCalledWith("setSearchPattern", null);
        expect(context.commit).toHaveBeenCalledWith("currentMessage/clear");
    });
    test("clear the current folder selection only if folder or filter changed", () => {
        selectFolder(context, { folderKey, filter: null });
        expect(context.commit).not.toHaveBeenCalledWith("messages/clearItems");
        expect(context.commit).not.toHaveBeenCalledWith("setCurrentFolder", expect.anything());
        expect(ContainerObserver.observe).not.toHaveBeenCalledWith("mailbox_records", folderUid);

        const another = ItemUri.encode("folderUid", "mailboxUid");
        selectFolder(context, { folderKey: another });
        expect(context.commit).toHaveBeenCalledWith("messages/clearItems");
        expect(context.commit).toHaveBeenCalledWith("setCurrentFolder", another);
        expect(ContainerObserver.observe).toHaveBeenCalledWith("mailbox_records", "folderUid");
    });
    test("always fetch messages for the selected folder", done => {
        const another = ItemUri.encode("folderUid", "mailboxUid");
        selectFolder(context, { folderKey })
            .then(() => {
                expect(context.dispatch).toHaveBeenNthCalledWith(1, "messages/list", {
                    sorted: context.state.sorted,
                    folderUid
                });
                expect(context.dispatch).toHaveBeenNthCalledWith(
                    2,
                    "messages/multipleByKey",
                    context.state.messages.itemKeys
                );
                context.dispatch.mockClear();
                return selectFolder(context, { folderKey: another });
            })
            .then(() => {
                expect(context.dispatch).toHaveBeenNthCalledWith(1, "messages/list", {
                    sorted: context.state.sorted,
                    folderUid: "folderUid"
                });
                expect(context.dispatch).toHaveBeenNthCalledWith(
                    2,
                    "messages/multipleByKey",
                    context.state.messages.itemKeys
                );
                done();
            });
    });
    test("fetch only the 100 first mails of the selected folder", done => {
        context.state.messages.itemKeys = new Array(200).fill(0).map((zero, i) => i);
        selectFolder(context, { folderKey }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith(
                "messages/multipleByKey",
                context.state.messages.itemKeys.slice(0, 100)
            );
            context.dispatch.mockClear();
            done();
        });
    });
    test("to watch the selected folder changes", () => {
        const another = ItemUri.encode("folderUid", "mailboxUid");
        selectFolder(context, { folderKey: another });
        expect(ContainerObserver.observe).toHaveBeenCalledWith("mailbox_records", "folderUid");
    });
    test("change message filter", () => {
        const folderKey = ItemUri.encode("key", "mailboxUid");
        selectFolder(context, { folderKey, filter: "unread" });
        expect(context.commit).toHaveBeenCalledWith("setMessageFilter", "unread");
        expect(context.commit).toHaveBeenCalledWith("messages/clearItems");
        expect(context.commit).toHaveBeenCalledWith("messages/clearParts");
    });
});
