import { areAllSelectedMessagesUnread } from "../../getters/areAllSelectedMessagesUnread";
import { Flag } from "@bluemind/email";

const mailSeen = {
    flags: [Flag.SEEN]
};

const mailUnseen = {
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

describe("[Mail-WebappStore][getters] : areAllMessagesSelected", () => {
    test("All messages are unread", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailUnseen,
            "2": mailUnseen,
            "3": mailUnseen
        });
        expect(areAllSelectedMessagesUnread(state, getters, rootState, rootGetters)).toBe(true);
    });

    test("All messages are unread (BEST EFFORT)", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailUnseen,
            "2": mailUnseen
        });
        expect(areAllSelectedMessagesUnread(state, getters, rootState, rootGetters)).toBe(true);
    });

    test("At least one message is not unread", () => {
        const { state, getters, rootState, rootGetters } = mockStore({
            "1": mailUnseen,
            "2": mailUnseen,
            "3": mailSeen
        });
        expect(areAllSelectedMessagesUnread(state, getters, rootState, rootGetters)).toBe(false);
    });
});
