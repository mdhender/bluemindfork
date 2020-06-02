import { areAllSelectedMessagesUnflagged } from "../../src/getters/areAllSelectedMessagesUnflagged";
import { Flag } from "@bluemind/email";

const mailFlagged = {
    value: {
        flags: [Flag.FLAGGED]
    }
};

const mailUnflagged = {
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

describe("[Mail-WebappStore][getters] : areAllSelectedMessagesUnflagged", () => {
    test("All messages are unflagged", () => {
        const state = mockState({
            "1": mailUnflagged,
            "2": mailUnflagged,
            "3": mailUnflagged
        });
        expect(areAllSelectedMessagesUnflagged(state)).toBe(true);
    });

    test("All messages are unflagged (BEST EFFORT)", () => {
        const state = mockState({
            "1": mailUnflagged,
            "2": mailUnflagged
        });
        expect(areAllSelectedMessagesUnflagged(state)).toBe(true);
    });

    test("At least one message is not unflagged", () => {
        const state = mockState({
            "1": mailUnflagged,
            "2": mailUnflagged,
            "3": mailFlagged
        });
        expect(areAllSelectedMessagesUnflagged(state)).toBe(false);
    });
});
