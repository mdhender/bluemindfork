import { ItemUri } from "@bluemind/item-uri";
import ContainerObserver from "@bluemind/containerobserver";
import { loadMessageList } from "../../src/actions/loadMessageList";

jest.mock("@bluemind/containerobserver");

const mailboxUid = "mailbox:uid";

const folderUid = "folder:uid",
    folderKey = ItemUri.encode(folderUid, mailboxUid);

const inboxUid = "inbox-uid",
    inboxKey = ItemUri.encode(inboxUid, mailboxUid);

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve([1, 2, 3])),
    commit: jest.fn(),
    state: {
        currentFolderKey: inboxKey,
        messages: { itemKeys: [1, 2, 3] },
        sorted: "up to down",
        messageFilter: null,
        draft: { parts: { attachments: [] } },
        search: {
            pattern: null
        }
    },
    getters: {
        my: {
            INBOX: { key: inboxKey, uid: inboxUid },
            mailboxUid: mailboxUid
        },
        "folders/getFolderByKey": jest
            .fn()
            .mockReturnValue({ uid: folderUid, key: folderKey, value: { fullName: "plop" } }),
        "folders/getFolderByPath": jest
            .fn()
            .mockReturnValue({ uid: folderUid, key: folderKey, value: { fullName: "plop" } }),
        mailshares: [{ root: "mailshare", mailboxUid: "mailshare_uid" }]
    }
};

describe("[Mail-WebappStore][actions] :  loadMessageList", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.state.currentFolderKey = folderKey;
        context.state.messages.itemKeys = [1, 2, 3];
        context.state.messageFilter = null;
        ContainerObserver.observe.mockClear();
        ContainerObserver.forget.mockClear();
    });
    test("locate folder by key", async () => {
        let messageQuery = { folder: ItemUri.encode("key") };
        await loadMessageList(context, messageQuery);
        expect(context.getters["folders/getFolderByKey"]).toHaveBeenCalledWith(messageQuery.folder);
        expect(context.getters["folders/getFolderByKey"]).toHaveBeenCalledTimes(1);
        expect(context.getters["folders/getFolderByPath"]).not.toHaveBeenCalled();
        context.getters["folders/getFolderByKey"].mockClear();
        messageQuery = { mailshare: ItemUri.encode("key") };
        await loadMessageList(context, messageQuery);
        expect(context.getters["folders/getFolderByKey"]).toHaveBeenCalledWith(messageQuery.mailshare);
        expect(context.getters["folders/getFolderByKey"]).toHaveBeenCalledTimes(1);
        expect(context.getters["folders/getFolderByPath"]).not.toHaveBeenCalled();
    });
    test("if locate by key fail, locate folder by path", async () => {
        let messageQuery = { folder: "/my/path" };
        context.getters["folders/getFolderByKey"].mockReturnValue(null);
        await loadMessageList(context, messageQuery);
        expect(context.getters["folders/getFolderByPath"]).toHaveBeenCalledWith(
            messageQuery.folder,
            context.getters.my.mailboxUid
        );
        expect(context.getters["folders/getFolderByPath"]).toHaveBeenCalledTimes(1);
        context.getters["folders/getFolderByPath"].mockClear();
        messageQuery = { mailshare: "mailshare/folder/" };
        await loadMessageList(context, messageQuery);
        expect(context.getters["folders/getFolderByPath"]).toHaveBeenCalledWith(
            messageQuery.mailshare,
            "mailshare_uid"
        );
        expect(context.getters["folders/getFolderByPath"]).toHaveBeenCalledTimes(1);
    });
    test("clear the current context", async () => {
        const messageQuery = {};
        await loadMessageList(context, messageQuery);
        expect(context.commit).toHaveBeenCalledWith("search/setStatus", "idle");
        expect(context.commit).toHaveBeenCalledWith("search/setPattern", undefined);
        expect(context.commit).toHaveBeenCalledWith("currentMessage/clear");
        expect(context.commit).toHaveBeenCalledWith("messages/clearParts");
        expect(context.commit).toHaveBeenCalledWith("messages/clearItems");
    });
    test("fetch messages on folder select folder", async () => {
        await loadMessageList(context, { folder: folderKey });
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "selectFolder", folderKey);
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "messages/list", {
            sorted: context.state.sorted,
            folderUid
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "messages/multipleByKey", context.state.messages.itemKeys);
    });

    test("set default folder to inbox by default", async () => {
        context.state.currentFolderKey = folderKey;
        await loadMessageList(context, {});
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "selectFolder", inboxKey);
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "messages/list", {
            sorted: context.state.sorted,
            folderUid: inboxUid
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "messages/multipleByKey", context.state.messages.itemKeys);
    });

    test("fetch only the 40 first mails of the selected folder", async () => {
        context.state.messages.itemKeys = new Array(200).fill(0).map((zero, i) => i);
        await loadMessageList(context, { folder: folderKey });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            context.state.messages.itemKeys.slice(0, 40)
        );
    });

    test("Call search action only if a search pattern is set", async () => {
        await loadMessageList(context, { folder: folderKey });
        expect(context.dispatch).not.toHaveBeenCalledWith("search/search", expect.anything());
        context.dispatch.mockClear();
        await loadMessageList(context, { folder: folderKey, search: '"search pattern"' });
        expect(context.dispatch).toHaveBeenCalledWith("search/search", {
            pattern: "search pattern",
            filter: undefined,
            folderKey: undefined
        });
        expect(context.dispatch).not.toHaveBeenCalledWith("message/list", expect.anything());
        expect(context.dispatch).not.toHaveBeenCalledWith("messages/multipleByKey", expect.anything());
    });

    test("Use filter for search or folder fetching if a filter is given", async () => {
        let messageQuery = { folder: folderKey, filter: "unread" };
        await loadMessageList(context, messageQuery);
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "messages/list", {
            sorted: context.state.sorted,
            folderUid,
            filter: messageQuery.filter
        });
        messageQuery = { folder: folderKey, search: '"search pattern"', filter: "unread" };
        await loadMessageList(context, messageQuery);
        expect(context.dispatch).toHaveBeenCalledWith("search/search", {
            pattern: "search pattern",
            filter: messageQuery.filter
        });
    });
});
