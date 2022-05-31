import Vue from "vue";
import store from "@bluemind/store";
import { extensions } from "@bluemind/extensions";
import injector from "@bluemind/inject";
import { AttachmentClient } from "@bluemind/attachment.api";
import FileHostingStore from "./store/";
import FileHostingCommand from "./FileHostingHandler";
import FileHostingModal from "~/components/FileHostingModal";
import FileHostingAttachment from "~/components/FileHostingAttachment";

Vue.component("file-hosting-modal", FileHostingModal);
Vue.component("filehosting-attachment", FileHostingAttachment);

extensions.register("webapp", "net.bluemind.webmodules.filehosting", {
    command: {
        name: "add-attachments",
        fn: FileHostingCommand,
        role: "canRemoteAttach"
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "filehosting-attachment",
        path: "message.attachment",
        role: "canRemoteAttach"
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "file-hosting-modal",
        path: "app.header"
    }
});

store.registerModule(["mail", "filehosting"], FileHostingStore);

injector.register({
    provide: "AttachmentPersistence",
    factory: () => {
        const userSession = injector.getProvider("UserSession").get();
        return new AttachmentClient(userSession.sid, userSession.domain);
    }
});
