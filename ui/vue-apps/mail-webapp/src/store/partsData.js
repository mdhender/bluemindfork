import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import Vue from "vue";

import { COMPUTE_QUOTE_NODES, FETCH_PART_DATA } from "~/actions";
import { RESET_PARTS_DATA, SET_PART_DATA, SET_QUOTE_NODES } from "~/mutations";
import { QUOTE_NODES } from "~/getters";
import QuoteHelper from "./helpers/QuoteHelper";
import { MessageHeader, extractHeaderValues } from "~/model/message";
import { VIEWER_CAPABILITIES, getPartsFromCapabilities } from "~/model/part";

export default {
    mutations: {
        [SET_PART_DATA]: (state, { messageKey, address, data }) => {
            if (!state.partsByMessageKey[messageKey]) {
                Vue.set(state.partsByMessageKey, messageKey, {});
            }
            Vue.set(state.partsByMessageKey[messageKey], address, data);
        },
        [RESET_PARTS_DATA]: state => {
            Vue.set(state.partsByMessageKey, {});
            Vue.set(state.quoteNodesByMessageKey, {});
        },
        [SET_QUOTE_NODES]: (state, { messageKey, quoteNodesByPartAddress }) => {
            if (!state.quoteNodesByMessageKey[messageKey]) {
                Vue.set(state.quoteNodesByMessageKey, messageKey, {});
            }
            if (quoteNodesByPartAddress) {
                Object.keys(quoteNodesByPartAddress).forEach(partAddress => {
                    Vue.set(
                        state.quoteNodesByMessageKey[messageKey],
                        partAddress,
                        quoteNodesByPartAddress[partAddress]
                    );
                });
            }
        }
    },

    getters: {
        [QUOTE_NODES]: state => (messageKey, partAddress) => state.quoteNodesByMessageKey[messageKey]?.[partAddress]
    },

    actions: {
        async [FETCH_PART_DATA]({ commit, state }, { folderUid, imapUid, inlines, messageKey }) {
            const service = inject("MailboxItemsPersistence", folderUid);
            const notLoaded = state.partsByMessageKey[messageKey]
                ? inlines.filter(
                      part => !Object.prototype.hasOwnProperty.call(state.partsByMessageKey[messageKey], part.address)
                  )
                : inlines;

            return Promise.all(
                notLoaded.map(async part => {
                    const blob = await service.fetch(imapUid, part.address, part.encoding, part.mime, part.charset);
                    const converted =
                        MimeType.isHtml(part) || MimeType.isText(part)
                            ? await convertAsText(blob, part)
                            : await convertToBase64(blob);
                    commit(SET_PART_DATA, { messageKey, data: converted, address: part.address });
                    return Promise.resolve();
                })
            );
        },

        async [COMPUTE_QUOTE_NODES](store, { message, conversationMessages }) {
            const {
                commit,
                dispatch,
                state: { partsByMessageKey }
            } = store;

            const messageParts = partsByMessageKey[message.key];

            if (messageParts) {
                // find quote using reply/forward separator
                let quoteNodesByPartAddress = QuoteHelper.findQuoteNodesUsingSeparator(messageParts);

                const atLeastOneQuoteNotFound = Object.values(quoteNodesByPartAddress).some(qn => qn === "NOT_FOUND");
                if (atLeastOneQuoteNotFound) {
                    // find quote using text comparison with related message
                    quoteNodesByPartAddress = await findQuoteNodesUsingTextComparison(
                        dispatch,
                        partsByMessageKey,
                        message,
                        conversationMessages,
                        messageParts,
                        quoteNodesByPartAddress
                    );
                }

                // clean-up
                Object.keys(quoteNodesByPartAddress).forEach(partAddress => {
                    if (quoteNodesByPartAddress[partAddress] === "NOT_FOUND") {
                        delete quoteNodesByPartAddress[partAddress];
                    }
                });

                commit(SET_QUOTE_NODES, { messageKey: message.key, quoteNodesByPartAddress });
            }
        }
    },

    state: {
        /**
         * Parts content keyed by message key and by part address.
         * @example partsByMessageKey["messageKey"]["partAddress"] = "partContent"
         */
        partsByMessageKey: {},
        /**
         * The found quote nodes of each part.
         * @example quoteNodesByMessageKey["messageKey"]["partAddress"] = [quoteNode1, quoteNode2]
         */
        quoteNodesByMessageKey: {}
    }
};

function convertAsText(blob, part) {
    return new Promise(resolve => {
        const reader = new FileReader();
        reader.readAsText(blob, part.charset);
        reader.addEventListener("loadend", e => {
            resolve(e.target.result);
        });
    });
}

function convertToBase64(blob) {
    const reader = new FileReader();
    reader.readAsDataURL(blob);
    return new Promise(resolve => {
        reader.onloadend = () => {
            resolve(reader.result);
        };
    });
}

async function findQuoteNodesUsingTextComparison(
    dispatch,
    partsByMessageKey,
    message,
    conversationMessages,
    messageParts,
    quoteNodesByPartAddress
) {
    const references = extractHeaderValues(message, MessageHeader.REFERENCES);

    if (references && references.length > 0) {
        const lastRef = references[references.length - 1];
        const relatedMessage = conversationMessages.find(m => m.messageId === lastRef);
        if (relatedMessage) {
            let relatedParts = partsByMessageKey[relatedMessage.key];
            if (!relatedParts) {
                const inlines = getPartsFromCapabilities(relatedMessage, VIEWER_CAPABILITIES);
                await dispatch(FETCH_PART_DATA, {
                    messageKey: relatedMessage.key,
                    folderUid: relatedMessage.folderRef.uid,
                    imapUid: relatedMessage.remoteRef.imapUid,
                    inlines: inlines.filter(part => MimeType.isHtml(part))
                });
                relatedParts = partsByMessageKey[relatedMessage.key];
            }

            if (relatedParts) {
                Object.keys(messageParts)
                    .filter(partKey => quoteNodesByPartAddress[partKey] === "NOT_FOUND")
                    .forEach(partKey => {
                        const messagePart = messageParts[partKey];
                        let quoteNodes;
                        const relatedMatchingContent = Object.values(relatedParts).find(relatedPart => {
                            quoteNodes = QuoteHelper.findQuoteNodesUsingTextComparison(messagePart, relatedPart);
                            return !!quoteNodes;
                        });

                        if (relatedMatchingContent) {
                            quoteNodesByPartAddress[partKey] = quoteNodes;
                        }
                    });
            }
        }
    }
    return quoteNodesByPartAddress;
}
