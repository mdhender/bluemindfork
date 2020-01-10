import { all as allAction } from "../../../src/MailboxFoldersStore/actions/all";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const result = [1, 2, 3];
const all = jest.fn().mockReturnValue(Promise.resolve(result));
const get = jest.fn().mockReturnValue({
    all
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[MailFoldersStore][actions] : all", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("call 'all' service for a given mailbox and mutate state with result", done => {
        const mailboxUid = "containerUid";
        allAction(context, mailboxUid).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storeItems", { items: result, mailboxUid });
            done();
        });
        expect(get).toHaveBeenCalledWith("containerUid");
        expect(all).toHaveBeenCalled();
    });
    test("fail if 'all' call fail", () => {
        const mailboxUid = "containerUid";
        all.mockReturnValueOnce(Promise.reject("Error!"));
        expect(allAction(context, mailboxUid)).rejects.toBe("Error!");
    });
});
