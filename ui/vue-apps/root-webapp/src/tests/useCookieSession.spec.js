import { mount } from "@vue/test-utils";
import store from "@bluemind/store";
import { useCookieSession } from "../composables/useCookieSession";

describe("useCookieSession", () => {
    let cookieDefault = { name: "defaultCookieName", value: { userPref: true } };
    let cookie = null;
    let cookieSession = null;
    describe("with default value", () => {
        it("should get the same cookie as the one set in the useCookieSession", () => {
            mount({
                store,
                cookieDefault,
                cookie,
                cookieSession,
                template: `<div></div>`,
                setup() {
                    cookieSession = useCookieSession("defaultCookieName1", cookieDefault.value);

                    return { cookieSession };
                },
                mounted() {
                    cookie = cookieSession.getValue();
                }
            });

            expect(cookie).toEqual(cookieDefault.value);
        });
        it("should not get the same cookie as the one set in the useCookieSession", () => {
            const newCookieValue = { name: "defaultCookieName", value: { newValue: "" } };
            mount({
                store,
                newCookieValue,
                cookieDefault,
                cookie,
                cookieSession,
                template: `<div></div>`,
                setup() {
                    cookieSession = useCookieSession("defaultCookieName2", cookieDefault.value);

                    return { cookieSession };
                },
                mounted() {
                    cookieSession.setValue(newCookieValue.value);
                    cookie = cookieSession.getValue();
                }
            });

            expect(cookie).toEqual(newCookieValue.value);
        });
    });
    describe("without default value", () => {
        it("should return null cookie without setting new value", () => {
            mount({
                store,
                cookieDefault,
                cookie,
                cookieSession,
                template: `<div></div>`,
                setup() {
                    cookieSession = useCookieSession("defaultCookieName3");

                    return { cookieSession };
                },
                mounted() {
                    cookie = cookieSession.getValue();
                }
            });

            expect(cookie).toBeFalsy();
        });
        it("should override and return new value set", () => {
            mount({
                store,
                cookieDefault,
                cookie,
                cookieSession,
                template: `<div></div>`,
                setup() {
                    cookieSession = useCookieSession("defaultCookieName4");

                    return { cookieSession };
                },
                mounted() {
                    cookieSession.setValue({ userPref: false });
                    cookie = cookieSession.getValue();
                }
            });

            expect(cookie).toEqual({ userPref: false });
        });
    });
});
