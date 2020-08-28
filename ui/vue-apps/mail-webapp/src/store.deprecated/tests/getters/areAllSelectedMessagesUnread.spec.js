import { areAllSelectedMessagesUnread } from "../../getters/areAllSelectedMessagesUnread";
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
    test("All messages are unread", () => {
        const { state, getters } = mockState({
            "1": mailUnseen,
            "2": mailUnseen,
            "3": mailUnseen
        });
        expect(areAllSelectedMessagesUnread(state, getters)).toBe(true);
    });

    test("All messages are unread (BEST EFFORT)", () => {
        const { state, getters } = mockState({
            "1": mailUnseen,
            "2": mailUnseen
        });
        expect(areAllSelectedMessagesUnread(state, getters)).toBe(true);
    });

    test("At least one message is not unread", () => {
        const { state, getters } = mockState({
            "1": mailUnseen,
            "2": mailUnseen,
            "3": mailSeen
        });
        expect(areAllSelectedMessagesUnread(state, getters)).toBe(false);
    });
});
