import { updateSeen } from "../../../src/MailboxItemsStore/actions/updateSeen";
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

jest.mock("@bluemind/inject");

const service = new MailboxItemsClient();
service.updateSeens.mockReturnValue(Promise.resolve());
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[MailItemsStore][actions] : updateSeen", () => {
    const messageId = "74515",
        folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23";
    const messageKey = ItemUri.encode(messageId, folderUid);

    beforeEach(() => {
        context.commit.mockClear();
    });

    test("call update seen for a given messageId and folderUid  and mutate state", done => {
        updateSeen(context, { messageKey, isSeen: false }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("updateSeen", { messageKey, isSeen: false });
            done();
        });
        expect(get).toHaveBeenCalledWith(folderUid);
        expect(service.updateSeens).toHaveBeenCalledWith([{ itemId: messageId, seen: false, mdnSent: false }]);

        updateSeen(context, { messageKey, isSeen: true }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("updateSeen", { messageKey, isSeen: true });
            done();
        });
        expect(get).toHaveBeenCalledWith(folderUid);
        expect(service.updateSeens).toHaveBeenCalledWith([{ itemId: messageId, seen: true, mdnSent: false }]);
    });

    test("fail if updateSeens call fail", () => {
        service.updateSeens.mockReturnValueOnce(Promise.reject("Error!"));
        expect(updateSeen(context, { messageKey, isSeen: false })).rejects.toBe("Error!");
    });
});
