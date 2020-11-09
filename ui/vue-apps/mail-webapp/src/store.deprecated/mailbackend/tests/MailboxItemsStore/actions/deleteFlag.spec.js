import { deleteFlag } from "../../../MailboxItemsStore/actions/deleteFlag";
import { Flag } from "@bluemind/email";
import { DELETE_FLAG } from "~actions";

const context = {
    dispatch: jest.fn().mockResolvedValue()
};

describe("[MailItemsStore][actions] : deleteFlag", () => {
    const mailboxItemFlag = Flag.SEEN;
    const messageKey = "key";

    beforeEach(() => {
        context.dispatch.mockClear();
    });

    test("call delete flag for a given messageId and folderUid  and mutate state", () => {
        deleteFlag(context, { messageKeys: [messageKey], mailboxItemFlag });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + DELETE_FLAG,
            { messageKeys: [messageKey], flag: mailboxItemFlag },
            { root: true }
        );
    });

    test("fail if deleteFlag call fail", async () => {
        context.dispatch.mockRejectedValue("Error");
        await expect(deleteFlag(context, { messageKeys: [messageKey], mailboxItemFlag })).rejects.toEqual("Error");
    });
});
