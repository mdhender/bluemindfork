import { remove } from "../../../src/MailboxFoldersStore/actions/remove";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const deleteById = jest.fn();
const get = jest.fn().mockReturnValue({
    deleteById
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
        deleteById.mockClear();
    });
    test("Basic", async () => {
        const folderKey = ItemUri.encode("folderId", "user.jdoe");
        await remove(context, folderKey);
        expect(context.commit).toHaveBeenCalledWith("removeItems", [folderKey]);
        expect(get).toHaveBeenCalledWith("user.jdoe");
        expect(deleteById).toHaveBeenCalledWith("folderInternalId");
        expect(context.commit).not.toHaveBeenCalledWith("storeItems", [folderKey]);
    });
    test("With error", async () => {
        deleteById.mockRejectedValue(new Error("ERROR"));
        const folderKey = ItemUri.encode("folderId", "user.jdoe");
        try {
            await remove(context, folderKey);
        } catch (e) {
            expect(context.commit).toHaveBeenCalledWith("removeItems", [folderKey]);
            expect(get).toHaveBeenCalledWith("user.jdoe");
            expect(deleteById).toHaveBeenCalledWith("folderInternalId");
            expect(context.commit).toHaveBeenCalledWith("storeItems", expect.anything());
        }
    });
});
