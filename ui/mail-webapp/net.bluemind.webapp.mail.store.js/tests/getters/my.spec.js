import { my } from "../../src/getters/my";
import { MailBoxBuilder } from "../../src/getters/helpers/MailBoxBuilder";
jest.mock("../../src/getters/helpers/MailBoxBuilder");
MailBoxBuilder.isMe.mockImplementation(container => container.name == "me");
MailBoxBuilder.build.mockReturnValue({ mailboxUid: "user.jane" });

const getters = {
    "folders/getDefaultFolders": jest.fn(),
    "folders/getFoldersByMailbox": jest.fn(),
    "mailboxes/containers": []
};
const state = { login: "jane@bluemind.net" };

describe("[Mail-WebappStore][getters] : my ", () => {
    beforeEach(() => {
        MailBoxBuilder.isMe.mockClear();
        MailBoxBuilder.build.mockClear();
        getters["folders/getDefaultFolders"].mockClear();
        getters["mailboxes/containers"] = [
            {
                name: "me",
                owner: "owner",
                type: "mailboxacl",
                ownerDirEntryPath: "domain/users/uid",
                writable: true
            },
            {
                name: "jane",
                owner: "owner",
                type: "mailboxacl",
                ownerDirEntryPath: "domain/users/uid",
                writable: true
            }
        ];
    });
    test("use MailboxBuilder to build my mailbox", () => {
        my(state, getters);
        expect(MailBoxBuilder.isMe).toHaveBeenCalled();
        expect(MailBoxBuilder.build).toHaveBeenCalledWith(getters["mailboxes/containers"][0], expect.anything());
    });
    test("return the defaults folder of the current user ", () => {
        my(state, getters);
        expect(getters["folders/getDefaultFolders"]).toHaveBeenCalledWith("user.jane");
    });

    test("build the mailbox even if no containers have been fetched ", () => {
        getters["mailboxes/containers"] = [];
        my(state, getters);
        const fake = { ownerDirEntryPath: "/users/", owner: "", writable: true, name: "jane" };
        expect(MailBoxBuilder.build).toHaveBeenCalledWith(fake, expect.anything());
    });
});
