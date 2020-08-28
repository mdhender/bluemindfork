import { count } from "../../../MailboxItemsStore/getters/count";

describe("[MailboxItemsStore][getters] : count ", () => {
    test("return number of items in list ", () => {
        const rootState = { mail: { messageList: { messageKeys: [1, 2] } } };
        expect(count(undefined, undefined, rootState)).toEqual(2);
        rootState.mail.messageList.messageKeys = [];
        expect(count(undefined, undefined, rootState)).toEqual(0);
    });
    test("return the size of the itemKeys not the number of item within", () => {
        const rootState = { mail: { messageList: { messageKeys: new Array(100) } } };
        expect(count(undefined, undefined, rootState)).toEqual(100);
    });
});
