import BmVuexStore from "./BmVuexStore";
import Vuex from "vuex";
import Vue from "vue";

Vue.use(Vuex);

describe("BmVuexStore: ", () => {
    let store, subscriber, data;

    beforeEach(() => {
        store = new BmVuexStore();
        data = "";
    });

    test("registerModule calls before subscribers", () => {
        subscriber = {
            before: () => {
                data = "before";
            }
        };
        store.subscribeModule(subscriber);
        store.registerModule("test", {});
        expect(data).toEqual("before");
    });

    test("registerModule calls after subscribers", () => {
        subscriber = {
            after: () => {
                data = "after";
            }
        };
        store.subscribeModule(subscriber);
        store.registerModule("test", {});
        expect(data).toEqual("after");
    });
});
