import { selectFolder } from "../../src/actions/selectFolder";
import { ItemUri } from "@bluemind/item-uri";
import ContainerObserver from "@bluemind/containerobserver";

jest.mock("@bluemind/containerobserver");

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    state: {
        currentFolderKey: "key",
        messages: { itemKeys: [1, 2, 3] },
        sorted: "up to down",
        messageFilter: null,
        draft: { parts: { attachments: [] } },
        search: {
            pattern: ""
        }
    }
};

const folderUid = "folder:uid",
    mailboxUid = "mailbox:uid";
const folderKey = ItemUri.encode(folderUid, mailboxUid);
describe("[Mail-WebappStore][actions] :  selectFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
        context.state.currentFolderKey = folderKey;
        ContainerObserver.observe.mockClear();
        ContainerObserver.forget.mockClear();
    });

    test("to set current folder only if folder has changed", async () => {
        await selectFolder(context, folderKey);
        expect(context.commit).not.toHaveBeenCalledWith("setCurrentFolder", expect.anything());
        const another = ItemUri.encode("folderUid", "mailboxUid");
        await selectFolder(context, another);
        expect(context.commit).toHaveBeenCalledWith("setCurrentFolder", another);
    });
    test("to watch the selected folder changes  only if folder has changed", async () => {
        await selectFolder(context, folderKey);
        expect(ContainerObserver.observe).not.toHaveBeenCalled();
        expect(ContainerObserver.forget).not.toHaveBeenCalled();
        const another = ItemUri.encode("folderUid", "mailboxUid");
        await selectFolder(context, another);
        expect(ContainerObserver.observe).toHaveBeenCalledWith("mailbox_records", "folderUid");
        expect(ContainerObserver.forget).toHaveBeenCalledWith("mailbox_records", folderUid);
    });

    test("load unread message count", async () => {
        await selectFolder(context, folderKey);
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUid);
    });
});
