import MailToolbarSelectedConversations from "../src/components/MailToolbar/MailToolbarSelectedConversations";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper, createStore, conversationKey } from "./testUtils";
jest.mock("@bluemind/styleguide/css/exports/avatar.scss", () => ({ 1: "#007bff" }));
jest.mock("~/store/api/apiConversations");
describe("MailToolbarSelectedConversations", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.vm).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should display 'mark unread' button not pressed if the message is read", async () => {
        const store = createStore();
        const mailbox = store.getters["mail/MY_MAILBOX"];
        const conversations = [store.state.mail.conversations.conversationByKey[conversationKey]];
        await store.dispatch("mail/MARK_CONVERSATIONS_AS_READ", {
            conversations,
            conversationsActivated: true,
            mailbox
        });
        const wrapper = createWrapper(MailToolbarSelectedConversations, { store });
        expect(wrapper.find(".mark-as-unread-captioned-icon-button").attributes("aria-pressed")).toBe("false");
    });

    test.skip("should display 'mark unread' button pressed if the message is unread", async () => {
        const store = createStore();
        const mailbox = store.getters["mail/MY_MAILBOX"];
        const conversations = [store.state.mail.conversations.conversationByKey[conversationKey]];
        await store.dispatch("mail/MARK_CONVERSATIONS_AS_UNREAD", {
            conversations,
            conversationsActivated: true,
            mailbox
        });
        const wrapper = createWrapper(MailToolbarSelectedConversations, { store });
        expect(wrapper.find(".mark-as-unread-captioned-icon-button").attributes("aria-pressed")).toBe("true");
    });
});
