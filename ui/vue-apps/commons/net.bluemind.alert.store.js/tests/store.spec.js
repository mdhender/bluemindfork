import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import initialStore, { ADD, AlertTypes, ERROR, REMOVE, SUCCESS, WARNING } from "../src";
import { LOADING } from "../src";
jest.useFakeTimers();

Vue.use(Vuex);

describe("alert", () => {
    const uid = "UID",
        name = "NAME",
        payload = "PayLoad",
        error = "Error",
        result = "Result",
        renderer = "Renderer";
    let store, alert, options;
    beforeEach(() => {
        alert = { uid, name, payload, error, result };
        options = { renderer };
        store = new Vuex.Store(cloneDeep(initialStore));
    });
    describe("actions", () => {
        describe("LOADING", () => {
            test("LOADING: Set alert in loading state", async () => {
                await store.dispatch(LOADING, { alert, options });
                jest.runAllTimers();
                expect(store.state[0]).toEqual(
                    expect.objectContaining({ uid, name, payload, renderer, type: AlertTypes.LOADING })
                );
            });

            test("LOADING: Loading alert are not dismissible", async () => {
                await store.dispatch(LOADING, { alert, options });
                jest.runAllTimers();
                expect(store.state[0]).toEqual(expect.objectContaining({ dismissible: false }));
            });

            test("LOADING: display is delayed", async () => {
                await store.dispatch(LOADING, { alert, options });
                expect(store.state.length).toEqual(0);
                jest.runAllTimers();
                expect(store.state.length).toEqual(1);
            });
        });
        describe("SUCCESS", () => {
            test("SUCCESS: Set alert in success state", async () => {
                await store.dispatch(SUCCESS, { alert, options });
                expect(store.state[0]).toEqual(
                    expect.objectContaining({ uid, name, payload, result, renderer, type: AlertTypes.SUCCESS })
                );
            });

            test("SUCCESS: alert is dismissible", async () => {
                await store.dispatch(SUCCESS, { alert, options });
                expect(store.state[0]).toEqual(expect.objectContaining({ dismissible: true }));
            });

            test("SUCCESS: alert is automatically discarded", async () => {
                await store.dispatch(SUCCESS, { alert, options });
                expect(store.state.length).toEqual(1);
                jest.runAllTimers();
                expect(store.state.length).toEqual(0);
            });
        });
        describe("ERROR", () => {
            test("ERROR: Set alert in error state", async () => {
                await store.dispatch(ERROR, { alert, options });
                expect(store.state[0]).toEqual(
                    expect.objectContaining({ uid, name, payload, error, renderer, type: AlertTypes.ERROR })
                );
            });

            test("ERROR: alert is dismissible", async () => {
                await store.dispatch(ERROR, { alert, options });
                expect(store.state[0]).toEqual(expect.objectContaining({ dismissible: true }));
            });
        });
        describe("WARNING", () => {
            test("WARNING: Set alert in waring state", async () => {
                await store.dispatch(WARNING, { alert, options });
                expect(store.state[0]).toEqual(
                    expect.objectContaining({ uid, name, payload, renderer, type: AlertTypes.WARNING })
                );
            });

            test("WARNING: alert is dismissible", async () => {
                await store.dispatch(WARNING, { alert, options });
                expect(store.state[0]).toEqual(expect.objectContaining({ dismissible: true }));
            });
        });
        describe("ADD", () => {
            test("ADD: add alert", async () => {
                await store.dispatch(ADD, { alert, options: {} });
                expect(store.state[0]).toEqual(expect.objectContaining(alert));
            });

            test("ADD: renderer option set alert renderer", async () => {
                await store.dispatch(ADD, { alert, options: { renderer } });
                expect(store.state[0]).toEqual(expect.objectContaining({ ...alert, renderer }));
            });

            test("ADD: dismissible option set alert dismissible state", async () => {
                await store.dispatch(ADD, { alert, options: { dismissible: true } });
                expect(store.state[0]).toEqual(expect.objectContaining({ ...alert, dismissible: true }));
            });
            test("ADD: delay option delay alert display", async () => {
                await store.dispatch(ADD, { alert, options: { delay: 1000 } });
                expect(store.state.length).toEqual(0);
                jest.runAllTimers();
                expect(store.state.length).toEqual(1);
            });

            test("ADD: countDown option automatically remove alert", async () => {
                await store.dispatch(ADD, { alert, options: { countDown: 1000 } });
                expect(store.state.length).toEqual(1);
                jest.runAllTimers();
                expect(store.state.length).toEqual(0);
            });
        });
        describe("REMOVE", () => {
            test("REMOVE: remove alert from store", async () => {
                store.state.push(alert);
                store.state.push({ ...alert, uid: "Another" });
                await store.dispatch(REMOVE, alert);
                expect(store.state.length).toEqual(1);
            });
            test("REMOVE: remove alerts from store", async () => {
                store.state.push(alert);
                store.state.push({ ...alert, uid: "Another" });
                store.state.push({ ...alert, uid: "Another One" });
                await store.dispatch(REMOVE, [alert, { ...alert, uid: "Another" }]);
                expect(store.state.length).toEqual(1);
            });
            test("REMOVE: clear timers", async () => {
                store.state.push(alert);
                await store.dispatch(ADD, { alert, options: { delay: 1000 } });
                await store.dispatch(REMOVE, alert);
                jest.runAllTimers();
                expect(store.state.length).toEqual(0);
            });
        });
    });
    describe("mutations", () => {
        test("ADD", () => {
            store.commit(ADD, alert);
            expect([...store.state]).toEqual([alert]);
        });
        test("ADD: Remove alert with same uid", () => {
            store.commit(ADD, alert);
            store.commit(ADD, alert);
            expect([...store.state]).toEqual([alert]);
        });
        test("ADD: Add alert if not same uid", () => {
            store.commit(ADD, alert);
            let alert2 = { ...alert, uid: "UID2" };
            store.commit(ADD, alert2);
            expect([...store.state]).toEqual([alert, alert2]);
        });
        test("REMOVE", () => {
            const alert1 = cloneDeep(alert);
            const alert2 = cloneDeep(alert);
            alert2.uid = "UID2";
            store.state.push(alert1);
            store.state.push(alert2);
            store.commit(REMOVE, uid);
            expect([...store.state]).toEqual([alert2]);
        });
    });
});
