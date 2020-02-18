import { Flag } from "@bluemind/email";
import { addFlag } from "../../../src/MailboxItemsStore/mutations/addFlag";

describe("[MailItemsStore][mutations] : addFlag", () => {
    test("add seen flag to mark a message as seen", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { flags: [] } } } };

        addFlag(state, { messageKey, mailboxItemFlag: Flag.SEEN });

        expect(state.items[messageKey].value.flags.includes(Flag.SEEN)).toBeTruthy();
    });
    test("do not add again the seen flag when a message is already marked as seen", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { flags: [Flag.SEEN] } } } };

        addFlag(state, { messageKey, mailboxItemFlag: Flag.SEEN });
        expect(state.items[messageKey].value.flags.length).toEqual(1);
    });
    test("do not change the other flags", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { flags: [Flag.FORWARDED, Flag.ANSWERED] } } } };
        addFlag(state, { messageKey, mailboxItemFlag: Flag.SEEN });
        expect(state.items[messageKey].value.flags).toEqual(expect.arrayContaining([Flag.FORWARDED, Flag.ANSWERED]));
    });
});