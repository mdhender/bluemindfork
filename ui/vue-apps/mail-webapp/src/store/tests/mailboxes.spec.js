import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockContainersClient } from "@bluemind/test-utils";
import { Verb } from "@bluemind/core.container.api";
import initialStore from "../mailboxes";
import aliceContainers from "./data/users/alice/containers";
import { MailboxType } from "../helpers/MailboxAdaptor";
import { ADD_MAILBOXES } from "~mutations";
import { FETCH_MAILBOXES } from "~actions";
import { MAILSHARES, MAILSHARE_KEYS, MY_MAILBOX, MY_MAILBOX_KEY } from "~getters";

const userId = "6793466E-F5D4-490F-97BF-DF09D3327BF4";

const containersService = new MockContainersClient();
inject.register({ provide: "ContainersPersistence", factory: () => containersService });
inject.register({ provide: "UserSession", use: { userId } });
Vue.use(Vuex);

describe("mailboxes store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(initialStore));
    });
    describe("mutations", () => {
        const { mutations } = initialStore;
        describe("ADD_MAILBOXES", () => {
            test("Add a mailbox", () => {
                const mailbox = { key: "1" };
                const stateForTest = {};
                mutations[ADD_MAILBOXES](stateForTest, [mailbox]);
                expect(stateForTest).toStrictEqual({ "1": mailbox });
            });
            test("Add multiple mailboxes ", () => {
                const mailboxes = [{ key: "1" }, { key: "123" }];
                const stateForTest = {};
                mutations[ADD_MAILBOXES](stateForTest, mailboxes);
                expect(stateForTest).toStrictEqual({ "1": mailboxes[0], "123": mailboxes[1] });
            });
        });
    });

    describe("action", () => {
        beforeEach(() => {
            containersService.getContainers.mockClear();
        });
        describe("FETCH_MAILBOXES", () => {
            test("Fetch a mailboxe", async () => {
                const mailbox = aliceContainers.find(container => {
                    return container.type === "mailboxacl";
                });

                containersService.getContainers.mockResolvedValueOnce([mailbox]);
                await store.dispatch(FETCH_MAILBOXES, []);
                expect(Object.keys(store.state).length).toEqual(1);
                expect(Object.values(store.state)[0]).toMatchObject({
                    name: mailbox.ownerDisplayname,
                    writable: mailbox.verbs.includes(Verb.Write)
                });
            });
            test("Fetch users mailboxes", async () => {
                const mailboxes = aliceContainers.filter(container => {
                    return container.type === "mailboxacl" && ["alice", "bob"].includes(container.name);
                });
                containersService.getContainers.mockResolvedValueOnce(mailboxes);
                await store.dispatch(FETCH_MAILBOXES, []);
                expect(Object.keys(store.state).length).toEqual(2);
                Object.values(store.state).forEach(mailbox => {
                    expect(mailbox.type).toEqual(MailboxType.USER);
                    expect(mailbox.remoteRef.uid).toEqual("user." + mailbox.owner);
                    expect(mailbox.root).toEqual("");
                });
            });
            test("Fetch mailshare mailboxes", async () => {
                const mailboxes = aliceContainers.filter(container => {
                    return container.type === "mailboxacl" && ["read.only", "read.write"].includes(container.name);
                });
                containersService.getContainers.mockResolvedValueOnce(mailboxes);
                await store.dispatch(FETCH_MAILBOXES, []);
                expect(Object.keys(store.state).length).toEqual(2);
                Object.values(store.state).forEach(mailbox => {
                    expect(mailbox.type).toEqual(MailboxType.MAILSHARE);
                    expect(mailbox.remoteRef.uid).toEqual(mailbox.owner);
                    expect(mailbox.root).toEqual(mailbox.name);
                });
            });
        });
    });

    describe("getters", () => {
        test("MY_MAILBOX", () => {
            store.state["1"] = {
                key: "1",
                type: "mailshares"
            };
            store.state["1"] = {
                key: "2",
                type: "users",
                owner: "unknown"
            };
            store.state["MY_REAL_MAILBOX_KEY"] = {
                key: "MY_REAL_MAILBOX_KEY",
                type: "users",
                owner: userId
            };
            expect(store.getters[MY_MAILBOX]).toEqual({ key: "MY_REAL_MAILBOX_KEY", type: "users", owner: userId });
        });
        test("MY_MAILBOX_KEY match the mailbox where owner is userId session", () => {
            store.state["1"] = {
                key: "1",
                type: "mailshares"
            };
            store.state["1"] = {
                key: "2",
                type: "users",
                owner: "unknown"
            };
            store.state["MY_REAL_MAILBOX_KEY"] = {
                key: "MY_REAL_MAILBOX_KEY",
                type: "users",
                owner: userId
            };
            expect(store.getters[MY_MAILBOX_KEY]).toEqual("MY_REAL_MAILBOX_KEY");
        });

        test("MAILSHARES", () => {
            store.state["1"] = {
                key: "1",
                type: MailboxType.MAILSHARE
            };
            store.state["2"] = {
                key: "2",
                type: MailboxType.USER
            };
            store.state["3"] = {
                key: "3",
                type: MailboxType.MAILSHARE
            };
            expect(store.getters[MAILSHARE_KEYS]).toEqual(["1", "3"]);
            expect(store.getters[MAILSHARES]).toEqual([
                { key: "1", type: "mailshares" },
                { key: "3", type: "mailshares" }
            ]);
        });
    });
});
