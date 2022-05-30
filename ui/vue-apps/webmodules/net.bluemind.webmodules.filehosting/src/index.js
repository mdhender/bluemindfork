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

// FIXME
// if (injector.getProvider("UserSession").get().roles.includes("canRemoteAttach")) {
extensions.register("webapp", "net.bluemind.webmodules.file", {
    command: {
        name: "add-attachments",
        fn: FileHostingCommand
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.file", {
    component: {
        name: "filehosting-attachment",
        path: "message.attachment"
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.file", {
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
// }
