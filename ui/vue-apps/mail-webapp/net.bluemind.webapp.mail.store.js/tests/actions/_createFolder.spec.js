import { $_createFolder } from "../../src/actions/$_createFolder";
import ItemUri from "@bluemind/item-uri";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve(ItemUri.encode("dummy", "dummy"))),
    getters: {
        my: { mailboxUid: "mailbox-uid" },
        "folders/folders": [],
        "folders/getFolderByKey": jest.fn().mockImplementation(key => {
            return context.getters["folders/folders"].find(f => f.key == key);
        })
    }
};

function createHierarchy(path, mailbox) {
    const hierarchy = path.split("/").filter(Boolean);
    const folders = [];
    const uid = () => Math.round(Math.random() * 100000);
    hierarchy.reduce((parent, name) => {
        const folder = {
            uid: uid(),
            value: {
                name: name,
                fullName: parent ? parent.value.fullName + "/" + name : name,
                parentUid: parent ? parent.uid : null
            }
        };
        folder.key = ItemUri.encode(folder.uid, mailbox);
        folders.push(folder);
        return folder;
    }, null);
    return folders;
}

describe("[Mail-WebappStore][actions] :  $_createFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
        context.getters.my = { mailboxUid: "mailbox-uid" };
        context.getters["folders/folders"] = [];
        context.getters["folders/getFolderByKey"].mockClear();
    });
    test("do not create folder if path already exists", done => {
        context.getters["folders/folders"] = createHierarchy("My/beautifull/hierarchy", context.getters.my.mailboxUid);
        let promise = context.getters["folders/folders"].reduce((promise, f) => {
            const folder = { value: { fullName: f.value.fullName, name: f.value.name } };
            return promise.then(() => $_createFolder(context, folder));
        }, Promise.resolve());
        promise.then(() => {
            expect(context.dispatch).not.toHaveBeenCalled();
            done();
        });
    });
    test("do not create folder if a key is provided", done => {
        context.getters["folders/folders"] = createHierarchy("My/beautifull/hierarchy", context.getters.my.mailboxUid);
        let promise = context.getters["folders/folders"].reduce((promise, f) => {
            const folder = { key: f.key, value: { fullName: f.value.fullName, name: f.value.name } };
            return promise.then(() => $_createFolder(context, folder).then(key => expect(key).toEqual(folder.key)));
        }, Promise.resolve());
        promise.then(() => {
            expect(context.dispatch).not.toHaveBeenCalled();
            done();
        });
    });
    test("create folder and return the folder key", done => {
        const folder = { value: { fullName: "myFolder", name: "myFolder" } };
        $_createFolder(context, folder).then(key => {
            expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
                name: "myFolder",
                parentUid: null,
                mailboxUid: context.getters.my.mailboxUid
            });
            expect(key).toEqual(ItemUri.encode("dummy", "dummy"));
            done();
        });
    });
    test("create folder within the correct hierarchy", done => {
        let folders = createHierarchy("My/beautifull/hierarchy", context.getters.my.mailboxUid);
        folders = folders.concat(createHierarchy("Another/one/to/dust", context.getters.my.mailboxUid));
        context.getters["folders/folders"] = folders;
        let parent = folders.find(f => f.value.fullName == "Another/one");
        let folder = { value: { fullName: "Another/one/again", name: "again" } };
        $_createFolder(context, folder)
            .then(() => {
                expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
                    name: "again",
                    parentUid: parent.uid,
                    mailboxUid: context.getters.my.mailboxUid
                });
                parent = folders.find(f => f.value.fullName == "My/beautifull");
                folder = { value: { fullName: "My/beautifull/things", name: "things" } };
                return $_createFolder(context, folder);
            })
            .then(() => {
                expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
                    name: "things",
                    parentUid: parent.uid,
                    mailboxUid: context.getters.my.mailboxUid
                });
                done();
            });
    });
    test("create all folder's parents", done => {
        let folders = createHierarchy("My", context.getters.my.mailboxUid);
        context.getters["folders/folders"] = folders;
        let folder = { value: { fullName: "Another/one/to", name: "again" } };
        context.dispatch.mockReturnValueOnce(Promise.resolve(ItemUri.encode("key1", context.getters.my.mailboxUid)));
        context.dispatch.mockReturnValueOnce(Promise.resolve(ItemUri.encode("key2", context.getters.my.mailboxUid)));
        context.dispatch.mockReturnValueOnce(Promise.resolve(ItemUri.encode("key3", context.getters.my.mailboxUid)));
        context.dispatch.mockReturnValueOnce(Promise.resolve(ItemUri.encode("key4", context.getters.my.mailboxUid)));
        context.dispatch.mockReturnValueOnce(Promise.resolve(ItemUri.encode("key5", context.getters.my.mailboxUid)));
        $_createFolder(context, folder)
            .then(key => {
                expect(context.dispatch).toHaveBeenNthCalledWith(1, "folders/create", {
                    name: "Another",
                    parentUid: null,
                    mailboxUid: context.getters.my.mailboxUid
                });
                expect(context.dispatch).toHaveBeenNthCalledWith(2, "folders/create", {
                    name: "one",
                    parentUid: "key1",
                    mailboxUid: context.getters.my.mailboxUid
                });
                expect(context.dispatch).toHaveBeenNthCalledWith(3, "folders/create", {
                    name: "to",
                    parentUid: "key2",
                    mailboxUid: context.getters.my.mailboxUid
                });
                expect(key).toEqual(ItemUri.encode("key3", context.getters.my.mailboxUid));
                folder = { value: { fullName: "My/beautifull/hierarchy", name: "again" } };
                return $_createFolder(context, folder);
            })
            .then(key => {
                expect(context.dispatch).toHaveBeenNthCalledWith(4, "folders/create", {
                    name: "beautifull",
                    parentUid: folders[0].uid,
                    mailboxUid: context.getters.my.mailboxUid
                });
                expect(context.dispatch).toHaveBeenNthCalledWith(5, "folders/create", {
                    name: "hierarchy",
                    parentUid: "key4",
                    mailboxUid: context.getters.my.mailboxUid
                });
                expect(key).toEqual(ItemUri.encode("key5", context.getters.my.mailboxUid));
                done();
            });
    });
    test("create folders in another mailbox", done => {
        let folders = createHierarchy("Mailshare2/", "a-mailbox");
        folders = folders.concat(createHierarchy("Mailshare1/subFolder", "another-mailbox-uid"));
        context.getters["folders/folders"] = folders;
        let folder = { value: { fullName: "Mailshare1/subFolder/folder", name: "folder" } };
        $_createFolder(context, folder).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
                name: "folder",
                parentUid: expect.anything(),
                mailboxUid: "another-mailbox-uid"
            });
            done();
        });
    });
});
