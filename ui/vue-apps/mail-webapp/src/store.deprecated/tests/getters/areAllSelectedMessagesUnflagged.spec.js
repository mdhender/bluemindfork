import { areAllSelectedMessagesUnflagged } from "../../getters/areAllSelectedMessagesUnflagged";
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
            selectedMessageKeys: ["1", "2", "3"]
        },
        getters: {
            "messages/getMessageByKey": key => mockedItems[key]
        }
    };
}

describe("[Mail-WebappStore][getters] : areAllSelectedMessagesUnflagged", () => {
    test("All messages are unflagged", () => {
        const { state, getters } = mockState({
            "1": mailUnflagged,
            "2": mailUnflagged,
            "3": mailUnflagged
        });
        expect(areAllSelectedMessagesUnflagged(state, getters)).toBe(true);
    });

    test("All messages are unflagged (BEST EFFORT)", () => {
        const { state, getters } = mockState({
            "1": mailUnflagged,
            "2": mailUnflagged
        });
        expect(areAllSelectedMessagesUnflagged(state, getters)).toBe(true);
    });

    test("At least one message is not unflagged", () => {
        const { state, getters } = mockState({
            "1": mailUnflagged,
            "2": mailUnflagged,
            "3": mailFlagged
        });
        expect(areAllSelectedMessagesUnflagged(state, getters)).toBe(false);
    });
});
