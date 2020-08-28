import { remove } from "../../../MailboxItemsStore/actions/remove";
import actionTypes from "../../../../../store/actionTypes";

jest.mock("@bluemind/inject");

const context = {
    dispatch: jest.fn()
};

describe("[MailItemsStore][actions] : remove", () => {
    const messageKey = "messageKey";

    beforeEach(() => {
        context.dispatch.mockClear();
    });

    test("call remove service for a given messageId and folderUid  and mutate state", () => {
        remove(context, messageKey);
        expect(context.dispatch).toHaveBeenCalledWith("mail/" + actionTypes.REMOVE_MESSAGES, messageKey, {
            root: true
        });
    });

    test("fail if deleteById call fail", async () => {
        context.dispatch.mockReturnValueOnce(Promise.reject("Error!"));
        await expect(remove(context, messageKey)).rejects.toBe("Error!");
    });
});
