import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import { TranslationRegistry } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import { AttachmentClient } from "@bluemind/attachment.api";
import FilehostingL10N from "./l10n";
import AddAttachmentsHandler from "~/handlers/AddAttachmentsHandler";
import RemoveAttachmentHandler from "~/handlers/RemoveAttachmentHandler";
import RenderlessFileItem from "~/components/RenderlessFileItem";
import RenderlessStore from "~/components/RenderlessStore";
import PreviewInvalid from "~/components/PreviewInvalid";
import CloudIcon from "~/components/CloudIcon";
import DetachButton from "~/components/DetachButton";
import ChooserButton from "~/components/ChooserButton";
import CopyToDriveItem from "~/components/OtherActionsItems/CopyToDriveItem";
import DetachItem from "~/components/OtherActionsItems/DetachItem";

TranslationRegistry.register(FilehostingL10N);

Vue.component("FhRenderlessFileItem", RenderlessFileItem);
Vue.component("CloudIcon", CloudIcon);
Vue.component("DetachButton", DetachButton);
Vue.component("ChooserButton", ChooserButton);
Vue.component("PreviewInvalid", PreviewInvalid);
Vue.component("CopyToDriveItem", CopyToDriveItem);
Vue.component("DetachItem", DetachItem);
Vue.component("FhRenderlessStore", RenderlessStore);

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
        name: "FhRenderlessFileItem",
        path: "message.file",
        priority: 10
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "CloudIcon",
        path: "attachment.infos.tags"
    }
});

extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "DetachButton",
        path: "composer.footer.toolbar",
        role: "canRemoteAttach"
    }
});
extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting.drive", {
    component: {
        name: "ChooserButton",
        path: "composer.footer.toolbar",
        role: "canUseFilehosting"
    }
});
extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "PreviewInvalid",
        path: "file.preview"
    }
});
extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting.drive", {
    component: {
        name: "CopyToDriveItem",
        path: "file.actions",
        role: "canUseFilehosting"
    }
});
extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "DetachItem",
        path: "file.actions",
        role: "canRemoteAttach"
    }
});
extensions.register("webapp.mail", "net.bluemind.webmodules.filehosting", {
    component: {
        name: "FhRenderlessStore",
        path: "app.header"
    }
});
injector.register({
    provide: "AttachmentPersistence",
    factory: () => {
        const userSession = injector.getProvider("UserSession").get();
        return new AttachmentClient(userSession.sid, userSession.domain);
    }
});
