import { $_createFolder } from "../../actions/$_createFolder";

const mailboxUid = "mailbox-uid";

function createHierarchy(path, mailbox) {
    const hierarchy = path.split("/").filter(Boolean);
    const folders = [];
    const uid = () => Math.round(Math.random() * 100000) + "";
    hierarchy.reduce((parent, name) => {
        const folder = {
            uid: uid(),
            name,
            path: parent ? parent.path + "/" + name : name,
            parent: parent ? parent.uid : null,
            mailbox
        };
        folder.key = folder.uid;
        folders.push(folder);
        return folder;
    }, null);
    const res = {};
    folders.forEach(f => (res[f.key] = f));
    return res;
}

describe("[Mail-WebappStore][actions] :  $_createFolder", () => {
    let context;

    beforeEach(() => {
        context = {
            commit: jest.fn(),
            dispatch: jest.fn().mockReturnValue(Promise.resolve("dummy")),
            rootState: {
                mail: {
                    folders: {}
                }
            }
        };
    });

    test("do not create folder if path already exists", async () => {
        context.rootState.mail.folders = createHierarchy("My/beautifull/hierarchy", mailboxUid);
        const folder = {
            path: "My/beautifull/hierarchy",
            name: "hierarchy"
        };
        await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).not.toHaveBeenCalled();
    });

    test("do not create folder if a key is provided", async () => {
        const folder = { key: "okok" };
        await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).not.toHaveBeenCalled();
    });

    test("create folder and return the folder key", async () => {
        const folder = { path: "myFolder", name: "myFolder" };
        const key = await $_createFolder(context, { folder, mailboxUid });

        expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
            name: "myFolder",
            parent: null,
            mailboxUid
        });
        expect(key).toEqual("dummy");
    });

    test("create folder within the correct hierarchy", async () => {
        let folders = createHierarchy("My/beautifull/hierarchy", mailboxUid);
        let otherFolders = createHierarchy("Another/one/to/dust", mailboxUid);
        context.rootState.mail.folders = { ...folders, ...otherFolders };

        let parent = Object.values(context.rootState.mail.folders).find(f => f.path === "Another/one");
        let folder = { path: "Another/one/again", name: "again" };
        await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
            name: "again",
            parent: parent.key,
            mailboxUid
        });

        parent = Object.values(context.rootState.mail.folders).find(f => f.path === "My/beautifull");
        folder = { path: "My/beautifull/things", name: "things" };
        await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
            name: "things",
            parent: parent.key,
            mailboxUid
        });
    });

    test("create all folder's parents", async () => {
        context.rootState.mail.folders = createHierarchy("My", mailboxUid);

        context.dispatch.mockReturnValueOnce(Promise.resolve("key1"));
        context.dispatch.mockReturnValueOnce(Promise.resolve("key2"));
        context.dispatch.mockReturnValueOnce(Promise.resolve("key3"));
        context.dispatch.mockReturnValueOnce(Promise.resolve("key4"));
        context.dispatch.mockReturnValueOnce(Promise.resolve("key5"));

        let folder = { path: "Another/one/to", name: "again" };

        let key = await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "folders/create", {
            name: "Another",
            parent: null,
            mailboxUid
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "folders/create", {
            name: "one",
            parent: "key1",
            mailboxUid
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "folders/create", {
            name: "to",
            parent: "key2",
            mailboxUid
        });
        expect(key).toEqual("key3");

        folder = { path: "My/beautifull/hierarchy", name: "again" };
        key = await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenNthCalledWith(4, "folders/create", {
            name: "beautifull",
            parent: Object.keys(context.rootState.mail.folders)[0],
            mailboxUid
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(5, "folders/create", {
            name: "hierarchy",
            parent: "key4",
            mailboxUid
        });
        expect(key).toEqual("key5");
    });

    test("create folders in another mailbox", async () => {
        let folders = createHierarchy("Mailshare2/", "a-mailbox");
        let otherFolders = createHierarchy("Mailshare1/subFolder", "another-mailbox-uid");
        context.rootState.mail.folders = { ...folders, ...otherFolders };

        let folder = { path: "Mailshare1/subFolder/folder", name: "folder" };
        await $_createFolder(context, { folder, mailboxUid: "another-mailbox-uid" });
        expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
            name: "folder",
            parent: expect.anything(),
            mailboxUid: "another-mailbox-uid"
        });
    });
});
