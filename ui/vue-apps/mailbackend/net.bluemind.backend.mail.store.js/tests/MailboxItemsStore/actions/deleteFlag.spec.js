import { deleteFlag } from "../../../src/MailboxItemsStore/actions/deleteFlag";
import { Flag } from "@bluemind/email";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

jest.mock("@bluemind/inject");

const service = new MailboxItemsClient();
service.deleteFlag.mockReturnValue(Promise.resolve());
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[MailItemsStore][actions] : deleteFlag", () => {
    const messageId = "74515",
        folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23",
        mailboxItemFlag = Flag.SEEN;
    const messageKey = ItemUri.encode(messageId, folderUid);

    beforeEach(() => {
        context.commit.mockClear();
    });

    test("call delete flag for a given messageId and folderUid  and mutate state", done => {
        deleteFlag(context, { messageKey, mailboxItemFlag }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("deleteFlag", { messageKey, mailboxItemFlag });
            done();
        });
        expect(get).toHaveBeenCalledWith(folderUid);
        expect(service.deleteFlag).toHaveBeenCalledWith([{ itemsId: [messageId], mailboxItemFlag }]);

        deleteFlag(context, { messageKey, mailboxItemFlag }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("deleteFlag", { messageKey, mailboxItemFlag });
            done();
        });
        expect(get).toHaveBeenCalledWith(folderUid);
        expect(service.deleteFlag).toHaveBeenCalledWith([{ itemsId: [messageId], mailboxItemFlag }]);
    });

    test("fail if deleteFlag call fail", () => {
        service.deleteFlag.mockReturnValueOnce(Promise.reject("Error!"));
        expect(deleteFlag(context, { messageKey, mailboxItemFlag })).rejects.toBe("Error!");
    });
});
