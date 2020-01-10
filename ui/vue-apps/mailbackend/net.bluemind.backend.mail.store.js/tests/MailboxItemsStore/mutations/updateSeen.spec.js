import { updateSeen } from "../../../src/MailboxItemsStore/mutations/updateSeen";

describe("[MailItemsStore][mutations] : updateSeen", () => {
    test("add seen flag to mark a message as seen", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { systemFlags: [] } } } };

        updateSeen(state, { messageKey, isSeen: true });

        expect(state.items[messageKey].value.systemFlags.includes("seen")).toBeTruthy();
    });
    test("do not add again the seen flag when a message is already marked as seen", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { systemFlags: ["seen"] } } } };

        updateSeen(state, { messageKey, isSeen: true });
        expect(state.items[messageKey].value.systemFlags.length).toEqual(1);
    });
    test("to remove the seen flag when a message is marked as unseen", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { systemFlags: ["seen"] } } } };

        updateSeen(state, { messageKey, isSeen: false });
        expect(state.items[messageKey].value.systemFlags.includes("seen")).not.toBeTruthy();
    });
    test("do not change the other flags", () => {
        const messageKey = "key1";
        const state = { items: { [messageKey]: { value: { systemFlags: ["dummy", "crazy"] } } } };
        updateSeen(state, { messageKey, isSeen: true });
        expect(state.items[messageKey].value.systemFlags).toEqual(expect.arrayContaining(["dummy", "crazy"]));
        updateSeen(state, { messageKey, isSeen: false });
        expect(state.items[messageKey].value.systemFlags).toEqual(expect.arrayContaining(["dummy", "crazy"]));
    });
});
