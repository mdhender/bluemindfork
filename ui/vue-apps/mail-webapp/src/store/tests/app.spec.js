import { state, mutations, getters } from "../app";

describe("folderList store", () => {
    describe("mutations", () => {
        test("SET_ACTIVE_FOLDER: define the active folder", () => {
            mutations.SET_ACTIVE_FOLDER(state, "1");
            expect(state.activeFolder).toEqual("1");

            mutations.SET_ACTIVE_FOLDER(state, "2");
            expect(state.activeFolder).toEqual("2");
        });
    });

    describe("getters", () => {
        test("CURRENT_MAILBOX: return mailbox object matching activeFolder", () => {
            state.mailboxes = {
                A: { key: "A" },
                B: { key: "B" }
            };
            state.folders = {
                1: { key: "1", mailbox: "B" },
                2: { key: "2", mailbox: "A" }
            };
            mutations.SET_ACTIVE_FOLDER(state, "1");
            expect(getters.CURRENT_MAILBOX(state)).toEqual({ key: "B" });
        });
    });
});
