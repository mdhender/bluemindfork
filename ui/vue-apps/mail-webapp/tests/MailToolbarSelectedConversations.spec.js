import MailToolbarSelectedConversations from "../src/components/MailToolbar/MailToolbarSelectedConversations";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper, createStore, messageKey } from "./testUtils";

describe("MailToolbarSelectedConversations", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.vm).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should display 'mark unread' button if the message is read", async () => {
        const store = createStore();
        await store.dispatch("mail/MARK_CONVERSATIONS_AS_READ", {
            conversations: { key: messageKey }
        });
        const wrapper = createWrapper(MailToolbarSelectedConversations, { store });
        expect(wrapper.find(".read").isVisible()).toBe(true);
        expect(wrapper.find(".unread").isVisible()).toBe(false);
    });

    test("should display 'mark read' button if the message is unread", async () => {
        const store = createStore();

        await store.dispatch("mail/MARK_CONVERSATIONS_AS_UNREAD", {
            conversations: { key: messageKey }
        });
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.find(".unread").isVisible()).toBe(false);
        expect(wrapper.find(".read").isVisible()).toBe(true);
    });
});
