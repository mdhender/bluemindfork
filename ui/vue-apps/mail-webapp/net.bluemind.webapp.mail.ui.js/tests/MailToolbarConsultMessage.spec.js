import MailToolbarConsultMessage from "../src/MailToolbar/MailToolbarConsultMessage";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper, createStore } from "./testUtils";

describe("MailToolbarConsultMessage", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarConsultMessage);
        expect(wrapper.isVueInstance()).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarConsultMessage);
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should display 'mark unread' button if the message is read", () => {
        const wrapper = createWrapper(MailToolbarConsultMessage);
        expect(wrapper.contains(".btn.read")).toBeTruthy();
        expect(!wrapper.contains(".btn.unread")).toBeTruthy();
    });

    test("should display 'mark read' button if the message is unread", () => {
        const storeWithReadMessage = createStore({
            modules: {
                "mail-webapp/currentMessage": {
                    getters: {
                        message: jest.fn(() => {
                            return { key: "", states: ["not-seen"] };
                        })
                    }
                }
            }
        });

        const wrapper = createWrapper(MailToolbarConsultMessage, { store: storeWithReadMessage });
        expect(wrapper.contains(".btn.unread")).toBeTruthy();
        expect(!wrapper.contains(".btn.read")).toBeTruthy();
    });
});
