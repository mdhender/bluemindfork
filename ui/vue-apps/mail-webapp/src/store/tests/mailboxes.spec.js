import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockContainersClient, MockOwnerSubscriptionsClient } from "@bluemind/test-utils";
import { Verb } from "@bluemind/core.container.api";
import initialStore from "../mailboxes";
import aliceContainers from "./data/users/alice/containers";
import { ADD_MAILBOXES } from "~/mutations";
import { FETCH_MAILBOXES } from "~/actions";
import { MAILBOXES, MAILSHARES, MAILSHARE_KEYS, MY_MAILBOX, MY_MAILBOX_KEY, USER_MAILBOXES } from "~/getters";
import { MailboxType } from "../../model/mailbox";

const aliceUid = "6793466E-F5D4-490F-97BF-DF09D3327BF4";
const bobUid = "AB6A2A90-04DA-4BD8-8E56-C4A11666E6CC";
const userId = aliceUid;

const containersService = new MockContainersClient();
inject.register({ provide: "ContainersPersistence", factory: () => containersService });
inject.register({ provide: "OwnerSubscriptionsPersistence", factory: () => new MockOwnerSubscriptionsClient() });
inject.register({ provide: "UserSession", use: { userId } });
inject.register({
    provide: "DirectoryPersistence",
    factory: () => ({
        getMultiple: () => [
            { uid: aliceUid, value: { email: "alice.coupeur@bluemind.net", displayName: "Alice Coupeur" } },
            { uid: bobUid, value: { email: "bob.leponge@bluemind.net", displayName: "Bob Leponge" } },
            {
                uid: "2814CC5D-D372-4F66-A434-89863E99B8CD",
                value: { email: "some@mails.org", displayName: "Some mails" }
            },
            {
                uid: "A78219B7-6F50-457D-BECA-4614865B3E2B",
                value: { email: "other@mails.org", displayName: "Others mails" }
            }
        ]
    })
});
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
            test("Fetch a mailbox", async () => {
                const mailbox = aliceContainers.find(container => {
                    return container.type === "mailboxacl";
                });

                containersService.getContainers.mockResolvedValueOnce([mailbox]);
                await store.dispatch(FETCH_MAILBOXES);
                expect(Object.keys(store.state).length).toEqual(1);
                expect(Object.values(store.state)[0]).toMatchObject({
                    writable: mailbox.verbs.includes(Verb.Write)
                });
            });
            test("Fetch users mailboxes", async () => {
                const mailboxes = aliceContainers.filter(container => {
                    return container.type === "mailboxacl" && ["alice", "bob"].includes(container.name);
                });
                containersService.getContainers.mockResolvedValueOnce(mailboxes);
                await store.dispatch(FETCH_MAILBOXES);
                expect(Object.keys(store.state).length).toEqual(2);
                Object.values(store.state).forEach(mailbox => {
                    expect(mailbox.type).toEqual(MailboxType.USER);
                    expect(mailbox.remoteRef.uid).toEqual("user." + mailbox.owner);
                    expect(mailbox.root).toEqual("");
                });

                const aliceMailbox = store.state["user." + aliceUid];
                expect(aliceMailbox.address).toEqual("alice.coupeur@bluemind.net");
                expect(aliceMailbox.dn).toEqual("Alice Coupeur");
                expect(aliceMailbox.name).toEqual("alice.coupeur@bluemind.net");

                const bobMailbox = store.state["user." + bobUid];
                expect(bobMailbox.address).toEqual("bob.leponge@bluemind.net");
                expect(bobMailbox.dn).toEqual("Bob Leponge");
                expect(bobMailbox.name).toEqual("bob.leponge@bluemind.net");
            });
            test("Fetch mailshare mailboxes", async () => {
                const mailboxes = aliceContainers.filter(container => {
                    return container.type === "mailboxacl" && ["read.only", "read.write"].includes(container.name);
                });
                containersService.getContainers.mockResolvedValueOnce(mailboxes);
                await store.dispatch(FETCH_MAILBOXES);
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
        test("MAILBOXES", () => {
            store.state["1"] = {
                key: "1",
                type: "mailshares"
            };
            store.state["2"] = {
                key: "2",
                type: "users",
                owner: "unknown"
            };
            store.state["3"] = {
                key: "3",
                type: "users",
                owner: userId
            };
            expect(store.getters[MAILBOXES]).toEqual([store.state["1"], store.state["2"], store.state["3"]]);
        });
        test("MY_MAILBOX", () => {
            store.state["1"] = {
                key: "1",
                type: "mailshares"
            };
            store.state["2"] = {
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

        test("MAILSHARES (sorted by dn)", () => {
            store.state["1"] = {
                key: "1",
                type: MailboxType.MAILSHARE,
                dn: "ccc"
            };
            store.state["2"] = {
                key: "2",
                type: MailboxType.USER,
                dn: "aaa"
            };
            store.state["3"] = {
                key: "3",
                type: MailboxType.MAILSHARE,
                dn: "bbb"
            };
            expect(store.getters[MAILSHARE_KEYS]).toEqual(["3", "1"]);
            expect(store.getters[MAILSHARES]).toEqual([
                { key: "3", type: "mailshares", dn: "bbb" },
                { key: "1", type: "mailshares", dn: "ccc" }
            ]);
        });
        test("USER_MAILBOXES", () => {
            store.state["1"] = {
                key: "1",
                type: "mailshares"
            };
            store.state["2"] = {
                key: "2",
                type: "users",
                owner: "unknown"
            };
            store.state["3"] = {
                key: "3",
                type: "users",
                owner: userId
            };
            expect(store.getters[USER_MAILBOXES]).toEqual([store.state["3"], store.state["2"]]);
        });
    });
});
