import { remove as removeAction } from "../../../src/MailboxItemsStore/actions/remove";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const deleteById = jest.fn().mockReturnValue(Promise.resolve());
const get = jest.fn().mockReturnValue({
    deleteById
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("MailItems store: remove action", () => {
    
    const messageId = "74515",
        folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23/17289";

    beforeEach(() => {
        context.commit.mockClear();
    });
    
    test("call remove for a given messageId and folderUid will call API and mutate state", done => {
        removeAction(context, { messageId, folderUid }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("removeItems", [messageId]);
            done();
        });
        expect(get).toHaveBeenCalledWith(folderUid);
        expect(deleteById).toHaveBeenCalledWith(messageId);
    });

    test("fail if deleteById call fail", () => {
        deleteById.mockReturnValueOnce(Promise.reject("Error!"));
        expect(removeAction(context, { messageId, folderUid })).rejects.toBe("Error!");
    });
});
