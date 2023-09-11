import { inject } from "@bluemind/inject";
import { messageUtils } from "@bluemind/mail";
import { SET_MESSAGE_STRUCTURE } from "~/mutations";
const { getLeafParts } = messageUtils;

export default async function updateMessageStructure({ state, commit }, { key, structure }) {
    cleanObsoleteAddresses({ state }, key, structure);
    commit(SET_MESSAGE_STRUCTURE, { messageKey: key, structure });
}

function cleanObsoleteAddresses({ state }, key, newStructure) {
    const oldMessage = state[key];
    const currentAddresses = getAllAddresses(newStructure);
    const oldAddresses = getAllAddresses(oldMessage.structure);
    const service = inject("MailboxItemsPersistence", oldMessage.folderRef.uid);
    oldAddresses.forEach(old => {
        if (!currentAddresses.includes(old)) {
            service.removePart(old);
        }
    });
}

function getAllAddresses(structure) {
    const parts = getLeafParts(structure);
    return parts.flatMap(part => (part.address ? part.address : []));
}
