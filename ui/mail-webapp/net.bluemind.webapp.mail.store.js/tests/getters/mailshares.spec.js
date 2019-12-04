import { mailshares } from "../../src/getters/mailshares";
import { MailBoxBuilder } from "../../src/getters/helpers/MailBoxBuilder";
jest.mock("../../src/getters/helpers/MailBoxBuilder");
MailBoxBuilder.isMailshare.mockImplementation((container, i) => i % 2 == 1);

const getters = {
    "mailboxes/containers": []
};
describe("[Mail-WebappStore][getters] : mailshares ", () => {
    beforeEach(() => {
        MailBoxBuilder.isMailshare.mockClear();
        MailBoxBuilder.build.mockClear();
        getters["mailboxes/containers"] = [
            { type: "mailboxacl", ownerDirEntryPath: "bm.lan/users/941ED8F6", owner: "941ED8F6", writable: true },
            {
                type: "mailboxacl",
                ownerDirEntryPath: "bm.lan/mailshares/XZDDZZ3",
                name: "mailshare1",
                owner: "XZDDZZ3",
                writable: true
            },
            { type: "calendar", ownerDirEntryPath: "bm.lan/users/33FCC9D6", owner: "33FCC9D6" },
            {
                type: "mailboxacl",
                ownerDirEntryPath: "bm.lan/mailshares/D5030EE3",
                name: "mailshare2",
                owner: "D5030EE3",
                writable: false
            },
            { type: "addressbook", ownerDirEntryPath: "bm.lan/mailshares/EQDDDZ1", owner: "EQDDDZ1" }
        ];
    });
    test("only return mailshare from the mailboxes store ", () => {
        mailshares(undefined, getters);
        expect(MailBoxBuilder.isMailshare).toHaveBeenCalled();
        expect(MailBoxBuilder.build).toHaveBeenCalledWith(getters["mailboxes/containers"][1], expect.anything());
        expect(MailBoxBuilder.build).toHaveBeenCalledWith(getters["mailboxes/containers"][3], expect.anything());
    });
});
