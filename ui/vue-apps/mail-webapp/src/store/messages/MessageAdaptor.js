import ItemUri from "@bluemind/item-uri";
import MessageStatus from "./MessageStatus";
import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

export default {
    fromMailboxItem(remote, { key, uid }) {
        return {
            key: ItemUri.encode(remote.internalId, key),
            folderRef: { key, uid },
            remoteRef: {
                imapUid: remote.value.imapUid,
                internalId: remote.internalId
            },
            status: MessageStatus.LOADED,
            flags: remote.value.flags,
            remote
        };
    },

    toMailboxItem(local) {
        return {
            ...local.remote,
            value: {
                ...local.remote.value,
                flags: local.flags
            }
        };
    },

    create(internalId, { key, uid }) {
        return {
            key: ItemUri.encode(internalId, key),
            folderRef: { key, uid },
            remoteRef: { internalId },
            status: MessageStatus.NOT_LOADED
        };
    },

    partialCopy(message, properties = []) {
        return cloneDeep(pick(message, properties.concat("key", "folderRef", "status", "remoteRef")));
    }
};
