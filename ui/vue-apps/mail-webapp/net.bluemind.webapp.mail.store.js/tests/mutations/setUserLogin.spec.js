import { setUserLogin } from "../../src/mutations/setUserLogin";

describe("[Mail-WebappStore][mutations] : setUserLogin", () => {
    test("update login state", () => {
        const state = { login: undefined };
        setUserLogin(state, "myLogin");
        expect(state.login).toEqual("myLogin");
    });
    test("do not mutate anything else", () => {
        const state = { login: undefined };
        setUserLogin(state, "myLogin");
        expect(Object.keys(state)).toEqual(["login"]);
    });
});
