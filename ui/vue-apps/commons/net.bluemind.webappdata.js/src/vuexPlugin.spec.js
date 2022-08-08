const mockRemoteValue = "remote_value";
let createCalled = false,
    updateCalled = false;
jest.mock("@bluemind/webappdata.api", () => ({
    WebAppDataClient() {
        return {
            create() {
                createCalled = true;
            },
            update() {
                updateCalled = true;
            },
            getByKey(key) {
                if (key === "blabla:my_prop") {
                    return { key, value: JSON.stringify(mockRemoteValue) };
                }
            }
        };
    }
}));
import store from "@bluemind/store";
import { defaultMutationType, getKey, privateMutationType } from "./vuexPlugin";

describe("AppData Vuex plugin: ", () => {
    let myModule;
    const delay = ms => new Promise(res => setTimeout(res, ms)); // allow to test async getByKey

    beforeEach(() => {
        createCalled = false;
        updateCalled = false;
        if (store.hasModule("blabla")) {
            store.unregisterModule("blabla");
        }
    });

    test("private mutation is created for each synced property", () => {
        myModule = { state: { myProp: null, anotherProp: "ok", synced: ["myProp", "anotherProp"] } };
        store.registerModule("blabla", myModule);
        expect(myModule.mutations).toBeDefined();

        const mutationType = privateMutationType("myProp");
        expect(myModule.mutations[mutationType]).toBeDefined();
        store.commit(mutationType, "initialized");
        expect(myModule.state.myProp).toEqual("initialized");

        const anotherMutationType = privateMutationType("anotherProp");
        expect(myModule.mutations[anotherMutationType]).toBeDefined();
        store.commit(anotherMutationType, "ko");
        expect(myModule.state.anotherProp).toEqual("ko");
    });

    test("if a remote app data exists, state property is initialized with private mutation", async () => {
        myModule = { state: { myProp: null, synced: ["myProp"] } };
        let mutationCalled = false;

        const privateMutation = privateMutationType("myProp");
        store.subscribe(mutation => {
            if (mutation.type === privateMutation) {
                mutationCalled = true;
            }
        });

        store.registerModule("blabla", myModule);

        await delay(1000);
        expect(myModule.state.myProp).toEqual(mockRemoteValue);
        expect(mutationCalled).toEqual(true);
    });

    test("if no mutation is defined by consumer, a default one is set", () => {
        myModule = { state: { myProp: null, synced: ["myProp"] } };
        store.registerModule("blabla", myModule);

        expect(Object.entries(myModule.mutations).length).toEqual(2);

        const defaultType = defaultMutationType("myProp");
        expect(myModule.mutations[defaultType]).toBeDefined();
    });

    test("when default mutation is called, remote app data is created or updated", () => {
        myModule = { state: { myProp: null, plop: "ok", synced: ["plop", "myProp"] } };
        store.registerModule("blabla", myModule);

        expect(createCalled).toEqual(false);
        let defaultType = defaultMutationType("plop");
        store.commit(defaultType, "updated");
        expect(createCalled).toEqual(true);

        expect(updateCalled).toEqual(false);
        defaultType = defaultMutationType("myProp");
        store.commit(defaultType, "updated");
        expect(updateCalled).toEqual(true);
    });

    test("when custom mutation are defined and called, remote app data is updated", () => {
        myModule = {
            state: { myProp: null, synced: { myProp: "MY_CUSTOM_MUTATION" } },
            mutations: {
                MY_CUSTOM_MUTATION: (state, value) => {
                    state.myProp = value / 10;
                }
            }
        };
        store.registerModule("blabla", myModule);

        const defaultType = defaultMutationType("myProp");
        expect(myModule.mutations[defaultType]).not.toBeDefined();

        expect(updateCalled).toEqual(false);
        store.commit("MY_CUSTOM_MUTATION", 10);
        expect(updateCalled).toEqual(true);
        expect(myModule.state.myProp).toEqual(1);
    });

    test("default appData key naming in function of store", () => {
        myModule = { state: { myAmazingProp: null, synced: ["myAmazingProp"] } };
        store.registerModule("grand-parent", { state: {} });
        store.registerModule(["grand-parent", "parent"], { state: {} });

        const modulePath = ["grand-parent", "parent", "son"];
        store.registerModule(modulePath, myModule);

        expect(getKey(modulePath, "myAmazingProp")).toEqual("grand_parent:parent:son:my_amazing_prop");
    });
});
