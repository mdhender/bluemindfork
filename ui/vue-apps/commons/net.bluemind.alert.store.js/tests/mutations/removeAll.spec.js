import { removeAll } from "../../src/mutations/removeAll";

describe("[AlertStore][mutations] : removeAll", () => {
    test("remove alert from the state", () => {
        const state = {
            alerts: [
                {
                    code: "PREVIOUS_ALERT",
                    uid: "454484e-d484eed"
                },
                {
                    code: "ALERT_TO_DELETE"
                }
            ]
        };

        removeAll(state);

        expect(state.alerts.length).toEqual(0);
    });
});
