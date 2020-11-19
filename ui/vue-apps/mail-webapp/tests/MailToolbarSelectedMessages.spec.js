import MailToolbarSelectedMessages from "../src/components/MailToolbar/MailToolbarSelectedMessages";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper, createStore, messageKey } from "./testUtils";
import { Flag } from "@bluemind/email";

describe("MailToolbarSelectedMessages", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.vm).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should display 'mark unread' button if the message is read", () => {
        const store = createStore();
        store.commit("mail/ADD_FLAG", { messages: [store.state.mail.messages[messageKey]], flag: Flag.SEEN });

        const wrapper = createWrapper(MailToolbarSelectedMessages, { store });
        expect(wrapper.find(".unread").isVisible()).toBe(true);
        expect(wrapper.find(".read").isVisible()).toBe(false);
    });

    test("should display 'mark read' button if the message is unread", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.find(".read").isVisible()).toBe(false);
        expect(wrapper.find(".unread").isVisible()).toBe(true);
    });
});
