import { remove } from "../../src/mutations/remove";

describe("[AlertStore][mutations] : remove", () => {
    
    test("remove alert from the state", () => {
        const alertUid = "fer45fe5-dze441";

        const state = {
            alerts: [{ 
                code: "PREVIOUS_ALERT",
                uid: "454484e-d484eed"
            },
            { 
                code: "ALERT_TO_DELETE",
                uid: alertUid
            }]
        };

        remove(state, alertUid);
        
        expect(state.alerts.length).toEqual(1);
        expect(state.alerts.find(a => a.uid == alertUid)).toEqual(undefined);
    });

});
