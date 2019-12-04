import Folder from "../../src/MailboxFoldersStore/Folder";

function createItem(path) {
    path = path.replace(/^\/?(.*)\/?/g, "$1");
    return {
        value: {
            name: path.split("/").pop(),
            fullName: path
        }
    };
}

describe("Folder", () => {
    test("to contain the item and a key", () => {
        const key = "myKeyMouse";
        const item = createItem("any", key);
        const folder = new Folder(key, item);
        expect(folder.key).toEqual(key);
        expect(folder).toEqual(expect.objectContaining(item));
    });
    test("match method must check if the folder name matches the pattern", () => {
        let folder = new Folder("key", createItem("myFolder"));
        expect(folder.match("myFolde")).toBeTruthy();
        expect(folder.match("Folder")).toBeTruthy();
        expect(folder.match("myFol")).toBeTruthy();
        expect(folder.match("yFolde")).toBeTruthy();
        expect(folder.match("notMyFolder")).not.toBeTruthy();
    });
    test("match method to be case insensitive", () => {
        let folder = new Folder("key", createItem("myFolder"));
        expect(folder.match("yfold")).toBeTruthy();
    });
    test("match method to match the path", () => {
        let folder = new Folder("key", createItem("/First/Second/myFolder"));
        expect(folder.match("first/second/myfolder")).toBeTruthy();
        expect(folder.match("second/myfolder")).toBeTruthy();
        expect(folder.match("second/myfol")).toBeTruthy();
        expect(folder.match("second/first/myfolder")).not.toBeTruthy();
    });
    test("match method trailing and ending slash prevent wildcard", () => {
        let folder = new Folder("key", createItem("/First/Second/myFolder"));
        expect(folder.match("/first/second/myfolder/")).toBeTruthy();
        expect(folder.match("/second/myfolder")).not.toBeTruthy();
        expect(folder.match("first/second/myfo/")).not.toBeTruthy();
    });

    test("compare sort default folders first", () => {
        const defaultFolders = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];
        let f2 = new Folder("key", createItem("a"));
        defaultFolders.forEach(name => {
            let f1 = new Folder("key", createItem(name));
            expect(f2.compare(f1)).toBeGreaterThan(0);
        });
    });
});
