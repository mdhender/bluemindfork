import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { Flag } from "@bluemind/email";
import storeData from "../";

import {
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD
} from "../types/getters";
import { MessageStatus } from "../../model/message";

Vue.use(Vuex);
describe("Mail store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeData));
    });
    describe("store.mutations", () => {
        test("SET_ACTIVE_FOLDER: define the active folder", () => {
            store.commit("SET_ACTIVE_FOLDER", "1");
            expect(store.state.activeFolder).toEqual("1");

            store.commit("SET_ACTIVE_FOLDER", "2");
            expect(store.state.activeFolder).toEqual("2");
        });
    });

    describe("store.getters", () => {
        test("CURRENT_MAILBOX: return mailbox object matching activeFolder", () => {
            store.state.mailboxes = {
                A: { key: "A" },
                B: { key: "B" }
            };
            store.state.folders = {
                1: { key: "1", mailboxRef: { key: "B" } },
                2: { key: "2", mailboxRef: { key: "A" } }
            };
            store.commit("SET_ACTIVE_FOLDER", "1");
            expect(store.getters.CURRENT_MAILBOX).toEqual({ key: "B" });
        });
        test("ALL_SELECTED_MESSAGES_ARE_UNREAD", () => {
            store.state.messages = {
                1: { flags: [], status: MessageStatus.LOADED },
                2: { flags: [Flag.SEEN], status: MessageStatus.NOT_LOADED },
                3: { flags: [Flag.SEEN], status: MessageStatus.LOADED },
                4: { flags: [], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNREAD]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNREAD]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNREAD]).toBeFalsy();
        });
        test("ALL_SELECTED_MESSAGES_ARE_READ", () => {
            store.state.messages = {
                1: { flags: [Flag.SEEN], status: MessageStatus.LOADED },
                2: { flags: [], status: MessageStatus.NOT_LOADED },
                3: { flags: [], status: MessageStatus.LOADED },
                4: { flags: [Flag.SEEN], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_READ]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_READ]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_READ]).toBeFalsy();
        });
        test("ALL_SELECTED_MESSAGES_ARE_FLAGGED", () => {
            store.state.messages = {
                1: { flags: [Flag.FLAGGED], status: MessageStatus.LOADED },
                2: { flags: [], status: MessageStatus.NOT_LOADED },
                3: { flags: [], status: MessageStatus.LOADED },
                4: { flags: [Flag.FLAGGED], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_FLAGGED]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_FLAGGED]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_FLAGGED]).toBeFalsy();
        });
        test("ALL_SELECTED_MESSAGES_ARE_UNFLAGGED", () => {
            store.state.messages = {
                1: { flags: [], status: MessageStatus.LOADED },
                2: { flags: [Flag.FLAGGED], status: MessageStatus.NOT_LOADED },
                3: { flags: [Flag.FLAGGED], status: MessageStatus.LOADED },
                4: { flags: [], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]).toBeFalsy();
        });
        test("ALL_MESSAGES_ARE_SELECTED", () => {
            store.state.messageList = { messageKeys: [1, 2, 3] };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_MESSAGES_ARE_SELECTED]).toBeFalsy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_MESSAGES_ARE_SELECTED]).toBeTruthy();
            store.state.selection = [];
            store.state.messageList = { messageKeys: [] };
            expect(store.getters[ALL_MESSAGES_ARE_SELECTED]).toBeFalsy();
        });
    });
});
