import { getDefaultFolders } from "../../../src/MailboxFoldersStore/getters/getDefaultFolders";

const getters = {
    getFolderByPath: jest.fn().mockReturnValue("dummy")
};
const defaults = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

describe("[MailFoldersStore][getters] : getDefaultFolders ", () => {
    beforeEach(() => {
        getters.getFolderByPath.mockClear();
    });
    test("returns getFolderByPath result for all default folder of mailbox ", () => {
        const returns = getDefaultFolders(null, getters)("container 1");
        defaults.forEach(folder => expect(getters.getFolderByPath).toHaveBeenCalledWith(folder, "container 1"));
        expect(returns).toEqual({
            INBOX: "dummy",
            SENT: "dummy",
            DRAFTS: "dummy",
            TRASH: "dummy",
            JUNK: "dummy",
            OUTBOX: "dummy"
        });
    });
    test("returns null if there is no default folders in mailbox", () => {
        getters.getFolderByPath.mockReturnValue(null);
        const returns = getDefaultFolders(null, getters)("container 1");
        expect(returns).toEqual({
            INBOX: null,
            SENT: null,
            DRAFTS: null,
            TRASH: null,
            JUNK: null,
            OUTBOX: null
        });
    });
});
