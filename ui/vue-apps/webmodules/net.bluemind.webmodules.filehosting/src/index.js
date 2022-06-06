import Vue from "vue";
import store from "@bluemind/store";
import { extensions } from "@bluemind/extensions";
import injector from "@bluemind/inject";
import { AttachmentClient } from "@bluemind/attachment.api";
import FileHostingStore from "./store/";
import AddAttachmentsHandler from "~/handlers/AddAttachmentsHandler";
import RemoveAttachmentHandler from "~/handlers/RemoveAttachmentHandler";
import FileHostingAttachment from "~/components/FileHostingAttachment";
import CloudIcon from "~/components/CloudIcon";

Vue.component("filehosting-attachment", FileHostingAttachment);
Vue.component("cloud-icon", CloudIcon);

extensions.register("webapp", "net.bluemind.webmodules.filehosting", {
    command: {
        name: "add-attachments",
        fn: AddAttachmentsHandler,
        role: "canRemoteAttach"
    }
});

extensions.register("webapp", "net.bluemind.webmodules.filehosting", {
    command: {
        name: "remove-attachment",
        fn: RemoveAttachmentHandler,
        role: "canRemoteAttach",
        after: true
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
        name: "cloud-icon",
        path: "attachment.infos.tags",
        role: "canRemoteAttach"
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
