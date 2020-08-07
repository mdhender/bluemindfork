import { areMessagesFiltered } from "../../getters/areMessagesFiltered";

const state = { messageFilter: null };

describe("[Mail-WebappStore][getters] : areMessagesFiltered", () => {
    test("Test no filter", () => {
        state.messageFilter = undefined;
        let result = areMessagesFiltered(state);
        expect(result).toBeFalsy();
    });
    test("Test 'all' filter", () => {
        state.messageFilter = "all";
        let result = areMessagesFiltered(state);
        expect(result).toBeFalsy();
    });
    test("Test 'unread' filter", () => {
        state.messageFilter = "unread";
        let result = areMessagesFiltered(state);
        expect(result).toBeTruthy();
    });
    test("Test 'anotherawesomefilter' filter", () => {
        state.messageFilter = "unread";
        let result = areMessagesFiltered(state);
        expect(result).toBeTruthy();
    });
});
