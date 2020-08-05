import { Flag } from "@bluemind/email";
import { deleteFlag } from "../../../src/MailboxItemsStore/mutations/deleteFlag";

describe("[MailItemsStore][mutations] : deleteFlag", () => {
    test("delete an existing seen flag", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { flags: [Flag.SEEN] } } } };

        deleteFlag(state, { messageKeys: [messageKey], mailboxItemFlag: Flag.SEEN });

        expect(state.items[messageKey].value.flags.length).toEqual(0);
    });
    test("delete a flag which doesnt exist dont fail ", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { flags: [Flag.ANSWERED] } } } };

        deleteFlag(state, { messageKeys: [messageKey], mailboxItemFlag: Flag.SEEN });
        expect(state.items[messageKey].value.flags.length).toEqual(1);
    });
    test("do not change the other flags", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { flags: [Flag.FORWARDED, Flag.ANSWERED, Flag.SEEN] } } } };
        deleteFlag(state, { messageKeys: [messageKey], mailboxItemFlag: Flag.SEEN });
        expect(state.items[messageKey].value.flags).toEqual(expect.arrayContaining([Flag.FORWARDED, Flag.ANSWERED]));
        expect(state.items[messageKey].value.flags.length).toEqual(2);
    });
});
