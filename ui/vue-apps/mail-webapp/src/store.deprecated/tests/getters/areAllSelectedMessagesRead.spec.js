import { areAllSelectedMessagesRead } from "../../getters/areAllSelectedMessagesRead";
import { Flag } from "@bluemind/email";

const mailSeen = {
    flags: [Flag.SEEN]
};

const mailUnseen = {
    flags: []
};

function mockState(mockedItems) {
    return {
        state: {
            selectedMessageKeys: ["1", "2", "3"]
        },
        getters: {
            "messages/getMessageByKey": key => mockedItems[key]
        }
    };
}

describe("[Mail-WebappStore][getters] : areAllMessagesSelected", () => {
    test("All messages are read", () => {
        const { state, getters } = mockState({
            "1": mailSeen,
            "2": mailSeen,
            "3": mailSeen
        });
        expect(areAllSelectedMessagesRead(state, getters)).toBe(true);
    });

    test("All messages are read (BEST EFFORT)", () => {
        const { state, getters } = mockState({
            "1": mailSeen,
            "2": mailSeen
        });
        expect(areAllSelectedMessagesRead(state, getters)).toBe(true);
    });

    test("At least one message is not read", () => {
        const { state, getters } = mockState({
            "1": mailSeen,
            "2": mailSeen,
            "3": mailUnseen
        });
        expect(areAllSelectedMessagesRead(state, getters)).toBe(false);
    });
});
