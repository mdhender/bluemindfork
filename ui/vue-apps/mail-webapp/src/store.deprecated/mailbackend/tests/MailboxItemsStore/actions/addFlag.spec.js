import { addFlag } from "../../../MailboxItemsStore/actions/addFlag";
import { Flag } from "@bluemind/email";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";
import actionTypes from "../../../../../store/actionTypes";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

jest.mock("@bluemind/inject");

const service = new MailboxItemsClient();
service.addFlag.mockReturnValue(Promise.resolve());
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockResolvedValue(),
    state: {
        items: {}
    }
};

describe("[MailItemsStore][actions] : addFlag", () => {
    const messageId = "74515",
        folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23",
        mailboxItemFlag = Flag.SEEN;
    const messageKey = ItemUri.encode(messageId, folderUid);

    beforeEach(() => {
        context.commit.mockClear();
    });

    test("call add flag for a given messageId and folderUid  and mutate state", () => {
        addFlag(context, { messageKeys: [messageKey], mailboxItemFlag });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + actionTypes.ADD_FLAG,
            { messageKeys: [messageKey], flag: mailboxItemFlag },
            { root: true }
        );
    });

    test("fail if addFlag call fail", async () => {
        context.dispatch.mockReturnValueOnce(Promise.reject("Error!"));
        await expect(addFlag(context, { messageKeys: [messageKey], mailboxItemFlag })).rejects.toEqual("Error!");
    });
});
