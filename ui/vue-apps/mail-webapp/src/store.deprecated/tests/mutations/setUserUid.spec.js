import { setUserUid } from "../../mutations/setUserUid";

describe("[Mail-WebappStore][mutations] : setUserLogin", () => {
    test("update login state", () => {
        const state = { userUid: undefined };
        setUserUid(state, "my-uid");
        expect(state.userUid).toEqual("my-uid");
    });
    test("do not mutate anything else", () => {
        const state = { userUid: undefined };
        setUserUid(state, "my-uid");
        expect(Object.keys(state)).toEqual(["userUid"]);
    });
});
