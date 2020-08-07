import { areAllSelectedMessagesFlagged } from "../../getters/areAllSelectedMessagesFlagged";
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

describe("[Mail-WebappStore][getters] : areAllSelectedMessagesFlagged", () => {
    test("All messages are flagged", () => {
        const state = mockState({
            "1": mailFlagged,
            "2": mailFlagged,
            "3": mailFlagged
        });
        expect(areAllSelectedMessagesFlagged(state)).toBe(true);
    });

    test("All messages are flagged (BEST EFFORT)", () => {
        const state = mockState({
            "1": mailFlagged,
            "2": mailFlagged
        });
        expect(areAllSelectedMessagesFlagged(state)).toBe(true);
    });

    test("At least one message is not flagged", () => {
        const state = mockState({
            "1": mailFlagged,
            "2": mailFlagged,
            "3": mailUnflagged
        });
        expect(areAllSelectedMessagesFlagged(state)).toBe(false);
    });
});
