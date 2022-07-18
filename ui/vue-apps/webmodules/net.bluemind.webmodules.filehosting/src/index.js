import Vue from "vue";
import store from "@bluemind/store";
import { extensions } from "@bluemind/extensions";
import injector from "@bluemind/inject";
import { AttachmentClient } from "@bluemind/attachment.api";
import FileHostingStore from "./store/";
import AddAttachmentsHandler from "~/handlers/AddAttachmentsHandler";
import RemoveAttachmentHandler from "~/handlers/RemoveAttachmentHandler";
import RenderlessFileItem from "~/components/RenderlessFileItem";
import CloudIcon from "~/components/CloudIcon";
import DetachButton from "~/components/DetachButton";

Vue.component("fh-renderless-file-item", RenderlessFileItem);
Vue.component("cloud-icon", CloudIcon);
Vue.component("detach-button", DetachButton);

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
        name: "fh-renderless-file-item",
        path: "message.file",
        priority: 10
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "cloud-icon",
        path: "attachment.infos.tags"
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "detach-button",
        path: "composer.footer.toolbar",
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
