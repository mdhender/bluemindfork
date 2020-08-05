import { all } from "../../../src/MailboxFoldersStore/actions/all";
import { FETCH_FOLDERS } from "@bluemind/webapp.mail.store";

const context = {
    rootState: { mail: { mailboxes: { containerUid: { key: "Mailbox" } } } },
    dispatch: jest.fn().mockResolvedValue()
};

describe("[MailFoldersStore][actions] : all", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
    });
    test("call 'all' service for a given mailbox and mutate state with undeleted folders only", async () => {
        await all(context, "containerUid");
        expect(context.dispatch).toHaveBeenCalledWith(FETCH_FOLDERS, { key: "Mailbox" }, { root: true });
    });
    test("fail if 'all' call fail", async () => {
        const mailboxUid = "containerUid";
        context.dispatch.mockRejectedValue("Error!");
        await expect(all(context, mailboxUid)).rejects.toBe("Error!");
    });
});
