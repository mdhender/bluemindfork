import injector from "@bluemind/inject";
import { FileHostingClient } from "@bluemind/filehosting.api";
import { AttachmentClient } from "@bluemind/attachment.api";

export default function registerDependencies() {
    registerUserSession();
    const userSession = injector.getProvider("UserSession").get();
    registerApiClients(userSession);
}

function registerUserSession() {
    injector.register({
        provide: "UserSession",
        use: window.bmcSessionInfos
    });
}

function registerApiClients(userSession) {
    injector.register({
        provide: "FileHostingPersistence",
        factory: () => {
            return new FileHostingClient(userSession.sid, userSession.domain);
        }
    });

    injector.register({
        provide: "AttachmentPersistence",
        factory: () => {
            return new AttachmentClient(userSession.sid, userSession.domain);
        }
    });
}
