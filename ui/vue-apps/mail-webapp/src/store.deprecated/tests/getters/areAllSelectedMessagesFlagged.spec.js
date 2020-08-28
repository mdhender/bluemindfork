import { areAllSelectedMessagesFlagged } from "../../getters/areAllSelectedMessagesFlagged";
import { Flag } from "@bluemind/email";

const mailFlagged = {
    flags: [Flag.FLAGGED]
};

const mailUnflagged = {
    flags: []
};

function mockState(mockedItems) {
    return {
        state: {
            selectedMessageKeys: ["1", "2", "3"],
            messages: {
                items: mockedItems
            }
        },
        getters: {
            "messages/getMessageByKey": key => mockedItems[key]
        }
    };
}

describe("[Mail-WebappStore][getters] : areAllSelectedMessagesFlagged", () => {
    test("All messages are flagged", () => {
        const { state, getters } = mockState({
            "1": mailFlagged,
            "2": mailFlagged,
            "3": mailFlagged
        });
        expect(areAllSelectedMessagesFlagged(state, getters)).toBe(true);
    });

    test("All messages are flagged (BEST EFFORT)", () => {
        const { state, getters } = mockState({
            "1": mailFlagged,
            "2": mailFlagged
        });
        expect(areAllSelectedMessagesFlagged(state, getters)).toBe(true);
    });

    test("At least one message is not flagged", () => {
        const { state, getters } = mockState({
            "1": mailFlagged,
            "2": mailFlagged,
            "3": mailUnflagged
        });
        expect(areAllSelectedMessagesFlagged(state, getters)).toBe(false);
    });
});
