//FIXME: Refactor this

import { html2text } from "@bluemind/html-utils";
import { Message, DraftStatus } from "@bluemind/backend.mail.store";
import { MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

/** Save the current draft: create it into Drafts box, delete the previous one. */
export function saveDraft({ commit, state, getters }) {
    const previousDraftId = state.draft.id;
    let service, draft, userSession;

    // only one saveDraft at a time
    return waitUntilDraftNotSaving(state.draft, 250, 5)
        .then(() => {
            // prepare the message content and initialize services
            commit("updateDraft", { status: DraftStatus.SAVING });

            // deep clone
            draft = JSON.parse(JSON.stringify(state.draft));

            const previousMessage = draft.previousMessage;
            const isAReply = !!previousMessage;

            delete draft.id;
            delete draft.status;
            delete draft.saveDate;

            if (previousMessage && previousMessage.content && !draft.content.includes(previousMessage.content)) {
                draft.content += "\n\n\n" + previousMessage.content;
            }

            if (isAReply) {
                if (previousMessage.messageId) {
                    draft.headers.push({ name: "In-Reply-To", values: [previousMessage.messageId] });
                    draft.references = [previousMessage.messageId].concat(previousMessage.references);
                } else {
                    draft.references = previousMessage.references;
                }
            }

            sanitize(draft);

            const draftbox = getters.my.DRAFTS;

            service = injector.getProvider("MailboxItemsPersistence").get(draftbox.uid);
            userSession = injector.getProvider("UserSession").get();
        })
        .then(() => {
            let partsToUpload = {};

            // FIXME cyrus wants \r\n, should do the replacement in the core, yes ?
            let content = draft.content.replace(/\r?\n/g, "\r\n");

            if (draft.type === "text") {
                partsToUpload[MimeType.TEXT_PLAIN] = content;
            } else if (draft.type === "html") {
                partsToUpload[MimeType.TEXT_HTML] = content;
                partsToUpload[MimeType.TEXT_PLAIN] = html2text(content).replace(/\r?\n/g, "\r\n");
            }

            // upload draft parts
            let addrParts = {};
            let promises = Object.entries(partsToUpload).map(uploadMe =>
                service.uploadPart(uploadMe[1]).then(addrPart => {
                    addrParts[uploadMe[0]] = addrPart;
                    return Promise.resolve();
                })
            );
            return Promise.all(promises).then(() => addrParts);
        })
        .then(addrParts => {
            // create the new draft in the draftbox
            let structure;
            let textPart = {
                mime: MimeType.TEXT_PLAIN,
                address: addrParts[MimeType.TEXT_PLAIN],
                encoding: "quoted-printable",
                charset: "utf-8"
            };

            if (draft.type === "text") {
                structure = textPart;
            } else if (draft.type === "html") {
                structure = {
                    mime: MimeType.MULTIPART_ALTERNATIVE,
                    children: [
                        textPart,
                        {
                            mime: MimeType.TEXT_HTML,
                            address: addrParts[MimeType.TEXT_HTML],
                            encoding: "quoted-printable",
                            charset: "utf-8"
                        }
                    ]
                };
            }
            const key = ItemUri.encode(draft.id, getters.my.DRAFTS.uid);
            return service.create(
                new Message(key, draft).toMailboxItem(
                    userSession.defaultEmail,
                    userSession.formatedName,
                    true,
                    structure
                )
            );
        })
        .then(itemIdentifier => {
            // update the draft properties
            draft.id = itemIdentifier.id;
            commit("updateDraft", { status: DraftStatus.SAVED, saveDate: new Date(), id: itemIdentifier.id });
            if (previousDraftId) {
                // delete the previous draft
                return service.deleteById(previousDraftId);
            }
            return Promise.resolve();
        })
        .then(() => {
            // return the draft identifier
            return draft.id;
        })
        .catch(() => {
            commit("updateDraft", { status: DraftStatus.SAVE_ERROR, saveDate: null });
        });
}

function sanitize(messageToSend) {
    if (messageToSend.subject === "") {
        messageToSend.subject = "(No subject)";
    }
    messageToSend.content = messageToSend.content.replace(/[\n\r]/g, String.fromCharCode(13, 10));
}

/**
 * Wait until the draft is not saving or a timeout is reached.
 * @param {*} draft the draft message
 * @param {Number} delayTime the initial delay time between two checks
 * @param {Number} maxTries the maximum number of checks
 * @param {Number} iteration DO NOT SET. Only used internally for recursivity
 */
function waitUntilDraftNotSaving(draft, delayTime, maxTries, iteration = 1) {
    if (draft.status == DraftStatus.SAVING) {
        return new Promise(resolve => setTimeout(() => resolve(draft.status), delayTime)).then(status => {
            if (status != DraftStatus.SAVING) {
                return Promise.resolve();
            } else {
                if (iteration < maxTries) {
                    // 'smart' delay: add 250ms each retry
                    return waitUntilDraftNotSaving(draft, delayTime + 250, maxTries, ++iteration);
                } else {
                    return Promise.reject("Timeout while waiting for the draft to be saved");
                }
            }
        });
    } else {
        return Promise.resolve();
    }
}
