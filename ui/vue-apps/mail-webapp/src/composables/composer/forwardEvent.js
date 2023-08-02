import cloneDeep from "lodash.clonedeep";
import store from "@bluemind/store";
import { PartsBuilder, MimeType } from "@bluemind/email";
import { partUtils } from "@bluemind/mail";
import { SET_MESSAGE_STRUCTURE } from "~/mutations";

const { getPartsFromCapabilities } = partUtils;
export async function setForwardEventStructure({ inlinePartsByCapabilities }, newMessage) {
    const calendarPartAddress = getPartsFromCapabilities({ inlinePartsByCapabilities }, [MimeType.TEXT_CALENDAR])?.pop()
        ?.address;

    const calendarPart = PartsBuilder.createCalendarRequestPart(calendarPartAddress);

    const structure = cloneDeep(newMessage.structure);
    let alternativePart;
    if (structure.mime === MimeType.MULTIPART_MIXED) {
        alternativePart = structure.children.find(({ mime }) => mime === MimeType.MULTIPART_ALTERNATIVE);
    } else {
        alternativePart = structure;
    }
    alternativePart.children.push(calendarPart);
    store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, { messageKey: newMessage.key, structure });
    return newMessage;
}
