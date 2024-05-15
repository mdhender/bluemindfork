<template>
    <mail-viewer-content-loading v-if="content === undefined" />
    <div v-else class="text-html-file-viewer">
        <slot :html="html" :styles="styles">
            <inline-style>{{ styles }}</inline-style>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div v-html="html"></div>
        </slot>
        <bm-icon-button
            v-if="isCollapseActive"
            size="sm"
            class="align-self-start mt-6 ml-3 mb-2"
            icon="3dots-horizontal"
            @click="collapse_ = false"
        />
    </div>
</template>

<script>
import partition from "lodash.partition";
import { mapActions, mapGetters } from "vuex";
import linkifyHtml from "linkifyjs/html";
import { MimeType, InlineImageHelper } from "@bluemind/email";
import { sanitizeHtml, blockRemoteImages } from "@bluemind/html-utils";
import { BmIconButton } from "@bluemind/ui-components";
import { messageUtils, partUtils } from "@bluemind/mail";
import brokenImageIcon from "~/../assets/brokenImageIcon.png";
import { FETCH_PART_DATA } from "~/actions";
import { CONVERSATION_MESSAGE_BY_KEY } from "~/getters";
import QuoteHelper from "~/store/helpers/QuoteHelper";
import InlineStyle from "~/components/InlineStyle";

import MailViewerContentLoading from "../MailViewerContentLoading";
import FileViewerMixin from "./FileViewerMixin";

const { isForward, computeParts } = messageUtils;
const { VIEWER_CAPABILITIES } = partUtils;

export default {
    name: "TextHtmlFileViewer",
    components: { BmIconButton, MailViewerContentLoading, InlineStyle },
    mixins: [FileViewerMixin],
    props: {
        collapse: { type: Boolean, default: true },
        relatedParts: { type: Array, required: true }
    },
    data() {
        return { collapse_: this.collapse && !isForward(this.message), collapsedDOM: undefined };
    },
    computed: {
        ...mapGetters("mail", [CONVERSATION_MESSAGE_BY_KEY]),

        blockImages() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        },
        content() {
            return this.$store.state.mail.partsData.partsByMessageKey[this.message.key]?.[this.file.address];
        },
        parsedDOM() {
            return new DOMParser().parseFromString(this.content, "text/html");
        },
        contentAsNode() {
            return this.isCollapseActive ? this.collapsedDOM : this.parsedDOM;
        },
        htmlWithImages() {
            const images = this.relatedParts.filter(part => MimeType.isImage(part) && part.contentId);

            const partsData = this.$store.state.mail.partsData.partsByMessageKey[this.message.key];
            const [localImages, remoteImages] = partition(images, i => partsData[i.address]);

            let insertionResult = InlineImageHelper.insertAsUrl(
                [this.contentAsNode.body.innerHTML],
                remoteImages,
                this.message.folderRef.uid,
                this.message.remoteRef.imapUid
            );
            let html = insertionResult.contentsWithImageInserted[0];

            insertionResult = InlineImageHelper.insertAsLocalUrl([html], localImages, partsData, this.message.key);
            html = insertionResult.contentsWithImageInserted[0];

            return html;
        },
        html() {
            let html = linkifyHtml(sanitizeHtml(this.htmlWithImages));
            if (this.blockImages) {
                html = blockRemoteImages(html);
            }
            return html;
        },
        styles() {
            return extractStyleNotInBody(this.contentAsNode) + BM_STYLE;
        },
        isCollapseActive() {
            return this.collapse_ && this.collapsedDOM;
        }
    },
    watch: {
        async parsedDOM(value) {
            if (value) {
                const conversationMessages = this.message.conversationRef
                    ? this.CONVERSATION_MESSAGE_BY_KEY(this.message.conversationRef.key)
                    : [this.message];
                const copy = value.cloneNode(true);
                const quoteNodes = await this.computeQuoteNodes(conversationMessages, copy);
                if (quoteNodes) {
                    this.collapsedDOM = QuoteHelper.removeQuotes(copy, quoteNodes);
                }
            } else {
                this.collapsedDOM = undefined;
            }
        }
    },
    async created() {
        await this.FETCH_PART_DATA({
            messageKey: this.message.key,
            folderUid: this.message.folderRef.uid,
            imapUid: this.message.remoteRef.imapUid,
            parts: [this.file]
        });
    },
    destroyed() {
        InlineImageHelper.cleanLocalImages(this.message.key);
    },
    methods: {
        ...mapActions("mail", { FETCH_PART_DATA }),

        async computeQuoteNodes(conversationMessages, htmlDoc) {
            if (messageUtils.isReply(this.message)) {
                const messageParts = this.$store.state.mail.partsData.partsByMessageKey[this.message.key];
                if (messageParts) {
                    let quoteNodes = QuoteHelper.findQuoteNodes(this.message, htmlDoc);
                    if (!quoteNodes?.length) {
                        // find quote using text comparison with related message
                        quoteNodes = await findQuoteNodesUsingTextComparison(
                            this.$store.dispatch,
                            this.$store.state.mail.partsData.partsByMessageKey,
                            this.message,
                            conversationMessages,
                            this.file,
                            this.relatedParts
                        );
                    }
                    return quoteNodes;
                }
            }
        }
    }
};

async function findQuoteNodesUsingTextComparison(
    dispatch,
    partsByMessageKey,
    message,
    conversationMessages,
    messagePart,
    relatedParts
) {
    const references = messageUtils.extractHeaderValues(message, messageUtils.MessageHeader.REFERENCES);

    if (references && references.length > 0) {
        const lastRef = references[references.length - 1];
        const relatedMessage = conversationMessages.find(m => m.messageId === lastRef);
        if (relatedMessage) {
            let relatedPartsContent = partsByMessageKey[relatedMessage.key];
            if (!relatedPartsContent && relatedParts.length) {
                await dispatch(FETCH_PART_DATA, {
                    messageKey: relatedMessage.key,
                    folderUid: relatedMessage.folderRef.uid,
                    imapUid: relatedMessage.remoteRef.imapUid,
                    parts: relatedParts
                });
                relatedPartsContent = partsByMessageKey[relatedMessage.key];
            }

            if (relatedPartsContent) {
                for (const relatedPart in Object.values(relatedPartsContent)) {
                    const quoteNodes = QuoteHelper.findQuoteNodesUsingTextComparison(messagePart, relatedPart);
                    if (quoteNodes) {
                        return quoteNodes;
                    }
                }
            }
        }
    }
}

function extractStyleNotInBody(doc) {
    let result = "";

    let rootStyle = doc.documentElement.getAttribute("style");
    if (rootStyle) {
        result += " body { " + rootStyle + "} ";
    }

    let bodyStyle = doc.body.getAttribute("style");
    if (bodyStyle) {
        result += " body { " + bodyStyle + "} ";
    }

    const headStyle = [...doc.head.getElementsByTagName("style")].reduce(
        (all, current) => all + " " + current.innerText,
        ""
    );

    result += " " + headStyle;

    return result;
}

const BM_STYLE = `
        a {
            color: var(--neutral-fg);
        }
        a:hover {
            color: var(--neutral-fg-hi1);
        }
        img.blocked-image {
            position: relative;
            min-height: 50px;
            min-width: 55px;
            display: inline-block;
            border: solid 1px var(--neutral-fg-lo1) !important;
            vertical-align: top;
        }
        .blocked-background {
            background-image: url(${brokenImageIcon});
            background-position: 7px 7px;
            background-repeat: no-repeat;
            border: solid 1px var(--neutral-fg-lo1) !important;
        }
        img.blocked-image:before {
            content: attr(alt);
            color: var(--neutral-fg-hi1);
            display: block;
            position: absolute;
            width: 100%;
            height: 100%;
            background: var(--surface);
            background-image: url(${brokenImageIcon});
            background-repeat: no-repeat;
            background-position: 7px 7px;
            padding: 9px 7px 7px 27px;
            box-sizing: border-box;
            overflow: hidden;
            text-overflow: ellipsis;
            text-align start;
            white-space: nowrap;
            font-family: Montserrat;
            font-style: normal;
            font-weight: normal;
            font-size: 14px;
        }

        a img.blocked-image:before {
            color: var(--secondary-fg) !important;
            text-decoration-line: underline;
        }

        blockquote {
            width: unset !important;
            margin-inline-end: unset !important;
        }`;
</script>
