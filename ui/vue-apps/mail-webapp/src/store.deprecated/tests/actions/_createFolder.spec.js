import { $_createFolder } from "../../actions/$_createFolder";
import UUIDGenerator from "@bluemind/uuid";
import { CREATE_FOLDER } from "~actions";
import { ADD_FOLDER, REMOVE_FOLDER } from "~mutations";

const mailboxUid = "mailbox-uid";
const anotherMailboxUid = "another-mailbox-uid";
const mockedGeneratedUid = "mocked:generated:uid";
const serverUid = "server:folder:uid";

function createHierarchy(path, mailbox) {
    const hierarchy = path.split("/").filter(Boolean);
    const folders = [];
    const uid = () => Math.round(Math.random() * 100000) + "";
    hierarchy.reduce((parent, name) => {
        const folder = {
            remoteRef: { uid: uid() },
            name,
            path: parent ? parent.path + "/" + name : name,
            parent: parent ? parent.key : null,
            mailboxRef: { ...mailbox }
        };
        folder.key = folder.remoteRef.uid;
        folders.push(folder);
        return folder;
    }, null);
    const res = {};
    folders.forEach(f => (res[f.key] = f));
    return res;
}

describe("[Mail-WebappStore][actions]: $_createFolder", () => {
    let context;

    beforeEach(() => {
        context = {
            commit: jest.fn(),
            dispatch: jest.fn(),
            rootState: {
                mail: {
                    folders: {},
                    mailboxes: {
                        [mailboxUid]: { key: mailboxUid },
                        [anotherMailboxUid]: { key: anotherMailboxUid }
                    }
                }
            }
        };
        context.dispatch.mockImplementation((actionName, { key, name, parent, mailbox }) => {
            context.rootState.mail.folders[key] = {
                key,
                remoteRef: { uid: serverUid },
                name,
                parent,
                mailboxRef: { key: mailbox.key }
            };
        });
        UUIDGenerator.generate = jest.fn().mockReturnValue(mockedGeneratedUid);
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
        const folder = { path: "myFolder" };
        const key = await $_createFolder(context, { folder, mailboxUid });

        expect(context.dispatch).toHaveBeenNthCalledWith(
            1,
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: null,
                name: "myFolder",
                key: expect.anything()
            },
            { root: true }
        );
        expect(context.commit).toHaveBeenNthCalledWith(1, "mail/" + REMOVE_FOLDER, mockedGeneratedUid, { root: true });
        expect(context.commit).toHaveBeenNthCalledWith(
            2,
            "mail/" + ADD_FOLDER,
            {
                key: serverUid,
                remoteRef: { uid: serverUid },
                mailboxRef: { key: mailboxUid },
                name: "myFolder",
                parent: null
            },
            { root: true }
        );
        expect(key).toEqual(serverUid);
    });

    test("create folder within the correct hierarchy", async () => {
        let folders = createHierarchy("My/beautifull/hierarchy", mailboxUid);
        let otherFolders = createHierarchy("Another/one/to/dust", mailboxUid);
        context.rootState.mail.folders = { ...folders, ...otherFolders };

        let parent = Object.values(context.rootState.mail.folders).find(f => f.path === "Another/one");
        let folder = { path: "Another/one/again" };
        await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: parent.key,
                name: "again",
                key: expect.anything()
            },
            { root: true }
        );

        parent = Object.values(context.rootState.mail.folders).find(f => f.path === "My/beautifull");
        folder = { path: "My/beautifull/things" };
        await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: parent.key,
                name: "things",
                key: expect.anything()
            },
            { root: true }
        );
    });

    test("create all folder's parents", async () => {
        context.rootState.mail.folders = createHierarchy("My", mailboxUid);

        UUIDGenerator.generate.mockReturnValueOnce("key1");
        UUIDGenerator.generate.mockReturnValueOnce("key2");
        UUIDGenerator.generate.mockReturnValueOnce("key3");
        UUIDGenerator.generate.mockReturnValueOnce("key4");
        UUIDGenerator.generate.mockReturnValueOnce("key5");

        context.dispatch.mockImplementation((actionName, { key, name, parent, mailbox }) => {
            context.rootState.mail.folders[key] = {
                key,
                remoteRef: { uid: key },
                name,
                parent,
                mailboxRef: { key: mailbox.key }
            };
        });

        let folder = { path: "Another/one/to" };

        let key = await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenNthCalledWith(
            1,
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: null,
                name: "Another",
                key: expect.anything()
            },
            { root: true }
        );

        expect(context.dispatch).toHaveBeenNthCalledWith(
            2,
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: "key1",
                name: "one",
                key: expect.anything()
            },
            { root: true }
        );
        expect(context.dispatch).toHaveBeenNthCalledWith(
            3,
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: "key2",
                name: "to",
                key: expect.anything()
            },
            { root: true }
        );
        expect(key).toEqual("key3");

        folder = { path: "My/beautifull/hierarchy", name: "again" };
        key = await $_createFolder(context, { folder, mailboxUid });
        expect(context.dispatch).toHaveBeenNthCalledWith(
            4,
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: Object.keys(context.rootState.mail.folders)[0],
                name: "beautifull",
                key: expect.anything()
            },
            { root: true }
        );
        expect(context.dispatch).toHaveBeenNthCalledWith(
            5,
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: mailboxUid },
                parent: "key4",
                name: "hierarchy",
                key: expect.anything()
            },
            { root: true }
        );
        expect(key).toEqual("key5");
    });

    test("create folders in another mailbox", async () => {
        let folders = createHierarchy("Mailshare2/", "a-mailbox");
        let otherFolders = createHierarchy("Mailshare1/subFolder", anotherMailboxUid);
        context.rootState.mail.folders = { ...folders, ...otherFolders };

        let folder = { path: "Mailshare1/subFolder/folder", name: "folder" };
        await $_createFolder(context, { folder, mailboxUid: anotherMailboxUid });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + CREATE_FOLDER,
            {
                mailbox: { key: anotherMailboxUid },
                parent: expect.anything(),
                name: "folder",
                key: expect.anything()
            },
            { root: true }
        );
    });
});
