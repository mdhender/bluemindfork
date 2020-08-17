import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockContainersClient, MockOwnerSubscriptionsClient } from "@bluemind/test-utils";
import { Verb } from "@bluemind/core.container.api";
import { state, mutations, actions, getters } from "../mailboxes";
import aliceContainers from "./data/users/alice/containers";
import { MailboxType } from "../helpers/MailboxAdaptor";

const userId = "6793466E-F5D4-490F-97BF-DF09D3327BF4";

const containersService = new MockContainersClient();
inject.register({ provide: "ContainersPersistence", factory: () => containersService });
inject.register({ provide: "SubscriptionPersistence", factory: () => new MockOwnerSubscriptionsClient() });
inject.register({
    provide: "UserSession",
    factory: () => {
        return { userId };
    }
});
Vue.use(Vuex);

describe("mailboxes store", () => {
    describe("mutations", () => {
        describe("ADD_MAILBOXES", () => {
            test("Add a mailbox", () => {
                const mailbox = { key: "1" };
                const stateForTest = {
                    mailboxes: {}
                };
                mutations.ADD_MAILBOXES(stateForTest, [mailbox]);
                expect(stateForTest).toStrictEqual({ mailboxes: { "1": mailbox } });
            });
            test("Add multiple mailboxes ", () => {
                const mailboxes = [{ key: "1" }, { key: "123" }];
                const stateForTest = {
                    mailboxes: {}
                };
                mutations.ADD_MAILBOXES(stateForTest, mailboxes);
                expect(stateForTest).toStrictEqual({ mailboxes: { "1": mailboxes[0], "123": mailboxes[1] } });
            });
        });
    });

    describe("action", () => {
        let store;
        beforeEach(() => {
            store = new Vuex.Store(
                cloneDeep({
                    state,
                    mutations,
                    actions
                })
            );
            containersService.getContainers.mockClear();
        });
        describe("FETCH_MAILBOXES", () => {
            test("Fetch a mailboxe", async () => {
                const mailbox = aliceContainers.find(container => {
                    return container.type === "mailboxacl";
                });

                containersService.getContainers.mockResolvedValueOnce([mailbox]);
                await store.dispatch("FETCH_MAILBOXES");
                expect(Object.keys(store.state.mailboxes).length).toEqual(1);
                expect(Object.values(store.state.mailboxes)[0]).toMatchObject({
                    name: mailbox.ownerDisplayname,
                    writable: mailbox.verbs.includes(Verb.Write)
                });
            });
            test("Fetch users mailboxes", async () => {
                const mailboxes = aliceContainers.filter(container => {
                    return container.type === "mailboxacl" && ["alice", "bob"].includes(container.name);
                });
                containersService.getContainers.mockResolvedValueOnce(mailboxes);
                await store.dispatch("FETCH_MAILBOXES");
                expect(Object.keys(store.state.mailboxes).length).toEqual(2);
                Object.values(store.state.mailboxes).forEach(mailbox => {
                    expect(mailbox.type).toEqual(MailboxType.USER);
                    expect(mailbox.uid).toEqual("user." + mailbox.owner);
                    expect(mailbox.root).toEqual("");
                });
            });
            test("Fetch mailshare mailboxes", async () => {
                const mailboxes = aliceContainers.filter(container => {
                    return container.type === "mailboxacl" && ["read.only", "read.write"].includes(container.name);
                });
                containersService.getContainers.mockResolvedValueOnce(mailboxes);
                await store.dispatch("FETCH_MAILBOXES");
                expect(Object.keys(store.state.mailboxes).length).toEqual(2);
                Object.values(store.state.mailboxes).forEach(mailbox => {
                    expect(mailbox.type).toEqual(MailboxType.MAILSHARE);
                    expect(mailbox.uid).toEqual(mailbox.owner);
                    expect(mailbox.root).toEqual(mailbox.name);
                });
            });
        });
    });

    describe("getters", () => {
        test("MY_MAILBOX", () => {
            const state = {
                mailboxes: {
                    "1": {
                        key: "1",
                        type: "mailshares"
                    },
                    "2": {
                        key: "2",
                        type: "users"
                    }
                }
            };
            const mockedGetters = { MY_MAILBOX_KEY: "2" };
            expect(getters["MY_MAILBOX"](state, mockedGetters)).toEqual({ key: "2", type: "users" });
        });
        test("MY_MAILBOX_KEY match the mailbox where owner is userId session", () => {
            const state = {
                mailboxes: {
                    "1": {
                        key: "1",
                        type: "mailshares"
                    },
                    "2": {
                        key: "2",
                        type: "users",
                        owner: "unknown"
                    },
                    MY_REAL_MAILBOX_KEY: {
                        key: "MY_REAL_MAILBOX_KEY",
                        type: "users",
                        owner: userId
                    }
                }
            };
            expect(getters["MY_MAILBOX_KEY"](state)).toEqual("MY_REAL_MAILBOX_KEY");
        });

        test("MAILSHARES", () => {
            const state = {
                mailboxes: {
                    "1": {
                        key: "1",
                        type: "mailshares"
                    },
                    "2": {
                        key: "2",
                        type: "users"
                    },
                    "3": {
                        key: "3",
                        type: "mailshares"
                    }
                }
            };
            const mockedGetters = { MAILSHARE_KEYS: ["1", "3"] };
            expect(getters["MAILSHARE_KEYS"](state)).toEqual(["1", "3"]);
            expect(getters["MAILSHARES"](state, mockedGetters)).toEqual([
                { key: "1", type: "mailshares" },
                { key: "3", type: "mailshares" }
            ]);
        });
    });
});
