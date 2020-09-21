import { areAllSelectedMessagesUnflagged } from "../../getters/areAllSelectedMessagesUnflagged";
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

describe("[Mail-WebappStore][getters] : areAllSelectedMessagesUnflagged", () => {
    test("All messages are unflagged", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailUnflagged,
            "2": mailUnflagged,
            "3": mailUnflagged
        });
        expect(areAllSelectedMessagesUnflagged(state, getters, rootState, rootGetters)).toBe(true);
    });

    test("All messages are unflagged (BEST EFFORT)", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailUnflagged,
            "2": mailUnflagged
        });
        expect(areAllSelectedMessagesUnflagged(state, getters, rootState, rootGetters)).toBe(true);
    });

    test("At least one message is not unflagged", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailUnflagged,
            "2": mailUnflagged,
            "3": mailFlagged
        });
        expect(areAllSelectedMessagesUnflagged(state, getters, rootState, rootGetters)).toBe(false);
    });
});
