import cloneDeep from "lodash.clonedeep";
import initialStore, { AlertTypes, CLEAR, ERROR, REMOVE, SUCCESS } from "../src";
import { LOADING } from "../src";

describe("alert", () => {
    const uid = "UID",
        name = "NAME",
        payload = "PayLoad",
        error = "Error",
        result = "Result",
        renderer = "Renderer";
    let store, fullAlert;
    beforeEach(() => {
        fullAlert = { uid, name, payload, error, renderer, result };
        store = cloneDeep(initialStore);
    });
    describe("LOADING", () => {
        test("LOADING: Set alert in loading state", () => {
            store.mutations[LOADING](store.state, fullAlert);
            expect(store.state).toEqual([{ uid, name, payload, renderer, type: AlertTypes.LOADING }]);
        });
        test("LOADING: Remove alert with same uid", () => {
            store.mutations[LOADING](store.state, fullAlert);
            store.mutations[LOADING](store.state, fullAlert);
            expect(store.state).toEqual([{ uid, name, payload, renderer, type: AlertTypes.LOADING }]);
        });
        test("LOADING: Add alert if not same uid", () => {
            store.mutations[LOADING](store.state, fullAlert);
            fullAlert.uid = "UID2";
            store.mutations[LOADING](store.state, fullAlert);
            expect(store.state).toEqual([
                { uid, name, payload, renderer, type: AlertTypes.LOADING },
                { uid: "UID2", name, payload, renderer, type: AlertTypes.LOADING }
            ]);
        });
    });
    describe("SUCCESS", () => {
        test("SUCCESS: Set alert in success state", () => {
            store.mutations[SUCCESS](store.state, fullAlert);
            expect(store.state).toEqual([{ uid, name, payload, renderer, result, type: AlertTypes.SUCCESS }]);
        });
        test("SUCCESS: Remove alert with same uid", () => {
            store.mutations[LOADING](store.state, fullAlert);
            store.mutations[SUCCESS](store.state, fullAlert);
            expect(store.state).toEqual([{ uid, name, payload, renderer, result, type: AlertTypes.SUCCESS }]);
        });
        test("SUCCESS: Add alert if not same uid", () => {
            store.mutations[LOADING](store.state, fullAlert);
            fullAlert.uid = "UID2";
            store.mutations[SUCCESS](store.state, fullAlert);
            expect(store.state).toEqual([
                { uid, name, payload, renderer, type: AlertTypes.LOADING },
                { uid: "UID2", name, result, payload, renderer, type: AlertTypes.SUCCESS }
            ]);
        });
    });
    describe("ERROR", () => {
        test("ERROR: Set alert in loading state", () => {
            store.mutations[ERROR](store.state, fullAlert);
            expect(store.state).toEqual([{ uid, name, payload, error, renderer, type: AlertTypes.ERROR }]);
        });
        test("ERROR: Remove alert with same uid", () => {
            store.mutations[LOADING](store.state, fullAlert);
            store.mutations[ERROR](store.state, fullAlert);
            expect(store.state).toEqual([{ uid, name, payload, error, renderer, type: AlertTypes.ERROR }]);
        });
        test("ERROR: Add alert if not same uid", () => {
            store.mutations[LOADING](store.state, fullAlert);
            fullAlert.uid = "UID2";
            store.mutations[ERROR](store.state, fullAlert);
            expect(store.state).toEqual([
                { uid, name, payload, renderer, type: AlertTypes.LOADING },
                { uid: "UID2", name, payload, error, renderer, type: AlertTypes.ERROR }
            ]);
        });
    });
    test("REMOVE", () => {
        const alert1 = cloneDeep(fullAlert);
        const alert2 = cloneDeep(fullAlert);
        alert2.uid = "UID2";
        store.state = [alert1, alert2];
        store.mutations[REMOVE](store.state, uid);
        expect(store.state).toEqual([alert2]);
    });
    test("CLEAR", () => {
        const alert1 = cloneDeep(fullAlert);
        const alert2 = cloneDeep(fullAlert);
        alert2.uid = "UID2";
        store.state = [alert1, alert2];
        store.mutations[CLEAR](store.state, uid);
        expect(store.state).toEqual([]);
    });
});
