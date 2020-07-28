import { remove } from "../../../src/MailboxItemsStore/actions/remove";
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

jest.mock("@bluemind/inject");

const multipleDeleteById = jest.fn().mockReturnValue(Promise.resolve());
const get = jest.fn().mockReturnValue({
    multipleDeleteById
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[MailItemsStore][actions] : remove", () => {
    const messageId = "74515",
        folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23";
    const messageKey = ItemUri.encode(messageId, folderUid);

    beforeEach(() => {
        context.commit.mockClear();
    });

    test("call remove service for a given messageId and folderUid  and mutate state", done => {
        remove(context, messageKey).then(() => {
            expect(context.commit).toHaveBeenCalledWith("removeItems", [messageKey]);
            done();
        });
        expect(get).toHaveBeenCalledWith(folderUid);
        expect(multipleDeleteById).toHaveBeenCalledWith([messageId]);
    });

    test("fail if deleteById call fail", async () => {
        multipleDeleteById.mockReturnValueOnce(Promise.reject("Error!"));
        await expect(remove(context, messageKey)).rejects.toBe("Error!");
    });
});
