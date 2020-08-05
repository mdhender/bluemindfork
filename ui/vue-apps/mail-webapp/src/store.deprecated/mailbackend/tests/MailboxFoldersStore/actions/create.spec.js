import { create } from "../../../src/MailboxFoldersStore/actions/create";
import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";
import { CREATE_FOLDER } from "@bluemind/webapp.mail.store";

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
        const key = await create(context, { name: "folder", parentUid: null, mailboxUid: "container" });
        expect(context.dispatch).toHaveBeenCalledWith(
            CREATE_FOLDER,
            {
                mailbox: { uid: "container" },
                parent: null,
                name: "folder",
                key: expect.anything()
            },
            { root: true }
        );
        expect(key).toEqual(ItemUri.encode("uid", "container"));
    });
});
