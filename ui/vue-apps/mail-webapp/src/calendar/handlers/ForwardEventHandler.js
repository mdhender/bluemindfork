import cloneDeep from "lodash.clonedeep";
import store from "@bluemind/store";
import router from "@bluemind/router";
import { PartsBuilder, MimeType } from "@bluemind/email";
import { partUtils } from "@bluemind/mail";
import { SET_MESSAGE_STRUCTURE } from "~/mutations";

const { getPartsFromCapabilities } = partUtils;

export default async function forwardEvent({ message, previousInfos }) {
    const { event } = router.currentRoute.query;
    if (event) {
        const calendarPartAddress = getPartsFromCapabilities(
            { inlinePartsByCapabilities: previousInfos.inlinePartsByCapabilities },
            [MimeType.TEXT_CALENDAR]
        )?.pop()?.address;

        const calendarPart = PartsBuilder.createCalendarRequestPart(calendarPartAddress);
        const structure = cloneDeep(message.structure);
        let alternativePart;
        if (structure.mime === MimeType.MULTIPART_MIXED) {
            alternativePart = structure.children.find(({ mime }) => mime === MimeType.MULTIPART_ALTERNATIVE);
        } else {
            alternativePart = structure;
        }
        alternativePart.children.push(calendarPart);
        store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, { messageKey: message.key, structure });
    }

    return { message, previousInfos };
}
