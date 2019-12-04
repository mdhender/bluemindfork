import { create as createAction } from "../../../src/MailboxFoldersStore/actions/create";
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

jest.mock("@bluemind/inject");

const createBasic = jest.fn().mockReturnValue(Promise.resolve({ uid: "UID" }));
const getComplete = jest.fn().mockReturnValue(Promise.resolve({ uid: "UID" }));
const get = jest.fn().mockReturnValue({
    createBasic,
    getComplete
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn(),
    getters: { getFolderByKey: jest.fn() }
};

describe("[MailFoldersStore][actions] : create", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.getters.getFolderByKey.mockClear();
        createBasic.mockClear();
    });
    test("call createBasics and getComplete with the given data", done => {
        createAction(context, { name: "folder", parentUid: null, mailboxUid: "container" }).then(() => {
            expect(getComplete).toHaveBeenCalledWith("UID");
            done();
        });
        expect(get).toHaveBeenCalledWith("container");
        expect(context.getters.getFolderByKey).not.toHaveBeenCalled();
        expect(createBasic).toHaveBeenCalledWith({
            name: "folder",
            fullName: "folder",
            parentUid: null,
            deleted: false
        });
    });
    test("call createBasics with folder fullpath calculated from parent path", () => {
        context.getters.getFolderByKey.mockReturnValueOnce({ value: { fullName: "parent/path" } });
        createAction(context, { name: "folder", parentUid: "parent", mailboxUid: "container" });
        expect(context.getters.getFolderByKey).toHaveBeenCalledWith(ItemUri.encode("parent", "container"));
        expect(createBasic).toHaveBeenCalledWith({
            name: "folder",
            fullName: "parent/path/folder",
            parentUid: "parent",
            deleted: false
        });
    });
    test("return folder uid", done => {
        createAction(context, { name: "folder", parentUid: null, mailboxUid: "container" }).then(value => {
            expect(value).toEqual(ItemUri.encode("UID", "container"));
            done();
        });
    });
});
