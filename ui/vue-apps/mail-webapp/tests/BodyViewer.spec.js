import i18n from "@bluemind/i18n";
import flushPromises from "flush-promises";
import inject from "@bluemind/inject";

inject.register({ provide: "UserSession", factory: () => ({ roles: "" }) });
inject.register({
    provide: "MailboxItemsPersistence",
    factory: () => ({
        fetch: () => new Blob()
    })
});
import { createStore } from "./testUtils";
import BodyViewer from "../src/components/MailViewer/BodyViewer";
import { shallowMount } from "@vue/test-utils";

describe("BodyViewer.spec", () => {
    test("Extract correct inlines and attachments", async () => {
        const structure = {
            mime: "multipart/mixed",
            children: [
                {
                    mime: "multipart/alternative",
                    children: [
                        { mime: "text/plain", address: "1.1", encoding: "quoted-printable", charset: "utf-8", size: 0 },
                        {
                            mime: "multipart/related",
                            address: "1.2",
                            children: [
                                {
                                    mime: "text/html",
                                    address: "1.2.1"
                                },
                                {
                                    mime: "image/jpeg",
                                    address: "1.2.2",
                                    contentId: "<D83890E2-CBE9-4886-BE69-5F6804CEFDC5@bluemind.net>",
                                    dispositionType: "INLINE"
                                }
                            ]
                        }
                    ]
                },
                {
                    mime: "application/pdf",
                    address: "2",
                    fileName: "document.pdf",
                    dispositionType: "ATTACHMENT"
                }
            ]
        };
        const wrapper = mountComponent(structure);

        await flushPromises();
        expect(wrapper.vm.inlineParts.length).toBe(2);
        expect(wrapper.vm.files.length).toBe(1);
    });

    test("unsupported part are displayed as attachment", async () => {
        const structure = {
            mime: "multipart/mixed",
            children: [
                {
                    mime: "application/octet-stream",
                    address: "1.2.2",
                    contentId: "<D83890E2-CBE9-4886-BE69-5F6804CEFDC5@bluemind.net>",
                    dispositionType: "INLINE"
                }
            ]
        };
        const wrapper = mountComponent(structure);
        await flushPromises();
        expect(wrapper.vm.files.length).toBe(1);
        expect(wrapper.vm.inlineParts.length).toBe(0);
    });
});

let mockedStore;
const message = {
    key: "truc",
    folderRef: { uid: "folderUid" },
    remoteRef: { imapUid: "imapUid" },
    composing: true,
    strucuture: null,
    conversationRef: {}
};

function mountComponent(structure) {
    message.structure = structure;
    mockedStore = createStore();
    const key = Object.keys(mockedStore.state.mail.conversations.conversationByKey).pop();
    message.conversationRef = { key };
    mockedStore.commit("mail/ADD_MESSAGES", { messages: [message] });
    return shallowMount(BodyViewer, {
        propsData: { message },
        store: mockedStore,
        i18n
    });
}
