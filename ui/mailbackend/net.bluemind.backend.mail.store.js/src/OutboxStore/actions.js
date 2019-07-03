import ServiceLocator from "@bluemind/inject";

// actions to retrieve all mails from a folder
export function flush() {
    return ServiceLocator.getProvider("OutboxPersistance")
        .get()
        .flush();
}
