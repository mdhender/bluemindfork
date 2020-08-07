import { create } from "../../../MailboxFoldersStore/actions/create";
import UUIDGenerator from "@bluemind/uuid";

jest.mock("@bluemind/uuid");

const context = {
    rootState: {
        mail: { mailboxes: { container: { uid: "container" } }, folders: { UUID: { uid: "uid" } } }
    },
    dispatch: jest.fn().mockResolvedValue(),
    commit: jest.fn()
};
UUIDGenerator.generate = jest.fn().mockReturnValue("UUID");

describe("[MailFoldersStore][actions] : create", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
    });
    test("call createBasics and getComplete with the given data", async () => {
        const key = await create(context, { name: "folder", parent: null, mailboxUid: "container" });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/CREATE_FOLDER",
            {
                mailbox: { uid: "container" },
                parent: null,
                name: "folder",
                key: expect.anything()
            },
            { root: true }
        );
        expect(key).toEqual("uid");
    });
});
