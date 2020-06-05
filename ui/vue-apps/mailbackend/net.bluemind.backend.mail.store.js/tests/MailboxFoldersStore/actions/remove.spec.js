import { remove } from "../../../src/MailboxFoldersStore/actions/remove";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const deepDelete = jest.fn();
const get = jest.fn().mockReturnValue({
    deepDelete
});
ServiceLocator.getProvider.mockReturnValue({
    get
});
const context = {
    commit: jest.fn(),
    getters: {
        getFolderByKey: jest.fn().mockImplementation(() => {
            return { uid: "folderId", internalId: "folderInternalId" };
        })
    }
};

describe("[MailFoldersStore][actions] : remove", () => {
    beforeEach(() => {
        context.commit.mockClear();
        deepDelete.mockClear();
    });
    test("Basic", async () => {
        const folderKey = ItemUri.encode("folderId", "user.jdoe");
        await remove(context, folderKey);
        expect(context.commit).toHaveBeenCalledWith("removeItems", [folderKey]);
        expect(get).toHaveBeenCalledWith("user.jdoe");
        expect(deepDelete).toHaveBeenCalledWith("folderInternalId");
        expect(context.commit).not.toHaveBeenCalledWith("storeItems", [folderKey]);
    });
    test("With error", async () => {
        deepDelete.mockRejectedValue(new Error("ERROR"));
        const folderKey = ItemUri.encode("folderId", "user.jdoe");
        try {
            await remove(context, folderKey);
        } catch (e) {
            expect(context.commit).toHaveBeenCalledWith("removeItems", [folderKey]);
            expect(get).toHaveBeenCalledWith("user.jdoe");
            expect(deepDelete).toHaveBeenCalledWith("folderInternalId");
            expect(context.commit).toHaveBeenCalledWith("storeItems", expect.anything());
        }
    });
});
