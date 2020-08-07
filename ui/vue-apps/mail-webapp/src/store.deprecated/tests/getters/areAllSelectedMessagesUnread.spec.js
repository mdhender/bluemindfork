import { areAllSelectedMessagesUnread } from "../../getters/areAllSelectedMessagesUnread";
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
    test("All messages are unread", () => {
        const state = mockState({
            "1": mailUnseen,
            "2": mailUnseen,
            "3": mailUnseen
        });
        expect(areAllSelectedMessagesUnread(state)).toBe(true);
    });

    test("All messages are unread (BEST EFFORT)", () => {
        const state = mockState({
            "1": mailUnseen,
            "2": mailUnseen
        });
        expect(areAllSelectedMessagesUnread(state)).toBe(true);
    });

    test("At least one message is not unread", () => {
        const state = mockState({
            "1": mailUnseen,
            "2": mailUnseen,
            "3": mailSeen
        });
        expect(areAllSelectedMessagesUnread(state)).toBe(false);
    });
});
