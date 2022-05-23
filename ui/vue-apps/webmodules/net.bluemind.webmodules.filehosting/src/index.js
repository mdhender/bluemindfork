import store from "@bluemind/store";
import injector from "@bluemind/inject";
import { AttachmentClient } from "@bluemind/attachment.api";
import FileHostingStore from "./store/";
import FileHostingCommand from "./FileHostingHandler";

// FIXME: to remove with new extension registration API
window.bmExtensions_["webapp"] = window.bmExtensions_["webapp"] || [];
window.bmExtensions_["webapp"].push({
    command: {
        name: "add-attachments",
        fn: FileHostingCommand
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
