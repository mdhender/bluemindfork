import { areAllSelectedMessagesRead } from "../../getters/areAllSelectedMessagesRead";
import { Flag } from "@bluemind/email";

const mailSeen = {
    value: {
        flags: [Flag.SEEN]
    }
};

const mailUnseen = {
    value: {
        flags: []
    }
};

function mockState(mockedItems) {
    return {
        selectedMessageKeys: ["1", "2", "3"],
        messages: {
            items: mockedItems
        }
    };
}

describe("[Mail-WebappStore][getters] : areAllMessagesSelected", () => {
    test("All messages are read", () => {
        const state = mockState({
            "1": mailSeen,
            "2": mailSeen,
            "3": mailSeen
        });
        expect(areAllSelectedMessagesRead(state)).toBe(true);
    });

    test("All messages are read (BEST EFFORT)", () => {
        const state = mockState({
            "1": mailSeen,
            "2": mailSeen
        });
        expect(areAllSelectedMessagesRead(state)).toBe(true);
    });

    test("At least one message is not read", () => {
        const state = mockState({
            "1": mailSeen,
            "2": mailSeen,
            "3": mailUnseen
        });
        expect(areAllSelectedMessagesRead(state)).toBe(false);
    });
});
