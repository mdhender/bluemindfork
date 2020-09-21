import { areAllSelectedMessagesFlagged } from "../../getters/areAllSelectedMessagesFlagged";
import { Flag } from "@bluemind/email";

const mailFlagged = {
    flags: [Flag.FLAGGED]
};

const mailUnflagged = {
    flags: []
};

function mockStore(mockedItems) {
    return {
        rootState: {
            mail: {
                messages: {
                    "1": mockedItems["1"],
                    "2": mockedItems["2"],
                    "3": mockedItems["3"]
                }
            }
        },
        state: {
            selectedMessageKeys: ["1", "2", "3"]
        },
        rootGetters: {
            "mail/isLoaded": key => mockedItems[key]
        }
    };
}

describe("[Mail-WebappStore][getters] : areAllSelectedMessagesFlagged", () => {
    test("All messages are flagged", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailFlagged,
            "2": mailFlagged,
            "3": mailFlagged
        });
        expect(areAllSelectedMessagesFlagged(state, getters, rootState, rootGetters)).toBe(true);
    });

    test("All messages are flagged (BEST EFFORT)", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailFlagged,
            "2": mailFlagged
        });
        expect(areAllSelectedMessagesFlagged(state, getters, rootState, rootGetters)).toBe(true);
    });

    test("At least one message is not flagged", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailFlagged,
            "2": mailFlagged,
            "3": mailUnflagged
        });
        expect(areAllSelectedMessagesFlagged(state, getters, rootState, rootGetters)).toBe(false);
    });
});
