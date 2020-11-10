<template>
    <div class="parts-viewer py-2">
        <template v-for="(part, index) in parts">
            <template v-if="isSupportedPart(part)">
                <hr v-if="index !== 0" :key="part.address + '-sepatator'" class="part-separator" />
                <component
                    :is="computePartComponent(part.mime)"
                    :key="part.address"
                    :value="getPartContent(part, index)"
                />
            </template>
        </template>
    </div>
</template>

<script>
import { mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";
import { MimeType, InlineImageHelper } from "@bluemind/email";

import { fetchAll } from "../../../model/message";
import { getPartsFromCapabilities } from "../../../model/part";
import ImagePartViewer from "./ImagePartViewer";
import TextHtmlPartViewer from "./TextHtmlPartViewer";
import TextPlainPartViewer from "./TextPlainPartViewer";
import { REMOVE_MESSAGE_PART_CONTENTS, SET_MESSAGE_PART_CONTENTS } from "~mutations";

const VIEWER_CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

export default {
    name: "PartsViewer",
    components: {
        ImagePartViewer,
        TextHtmlPartViewer,
        TextPlainPartViewer
    },
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            parts: [],
            htmlWithBlobs: []
        };
    },
    computed: {
        ...mapState("mail", ["messages"]),
        message() {
            return this.messages[this.messageKey];
        }
    },
    watch: {
        messageKey: {
            handler: async function (newKey, oldKey) {
                if (oldKey) {
                    this.cleanPartsContent(oldKey);
                }

                const inlines = getPartsFromCapabilities(this.message, VIEWER_CAPABILITIES);

                // FIXME: move fetchAll as an action
                const service = inject("MailboxItemsPersistence", this.message.folderRef.uid);
                const contents = await fetchAll(this.message.remoteRef.imapUid, service, inlines, false);
                this.SET_MESSAGE_PART_CONTENTS({ key: this.message.key, contents, parts: inlines });

                const html = inlines.filter(MimeType.isHtml);
                const images = inlines.filter(part => MimeType.isImage(part) && part.contentId);
                const insertionResult = InlineImageHelper.insertInlineImages(
                    html.map(part => this.message.partContentByAddress[part.address]),
                    images,
                    this.message.partContentByAddress
                );
                const others = inlines.filter(
                    part => part.mime !== MimeType.TEXT_HTML && !insertionResult.imageInlined.includes(part.contentId)
                );
                this.htmlWithBlobs = insertionResult.contentsWithBlob;
                this.parts = [...html, ...others];
            },
            immediate: true
        }
    },
    destroyed() {
        this.cleanPartsContent(this.messageKey);
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_PART_CONTENTS, REMOVE_MESSAGE_PART_CONTENTS }),
        isSupportedPart(part) {
            return MimeType.isHtml(part) || MimeType.isText(part) || MimeType.isImage(part);
        },
        computePartComponent(mimeType) {
            let name;
            if (MimeType.isImage({ mime: mimeType })) {
                name = "Image";
            } else {
                name = mimeType
                    .split("/")
                    .map(subtype => subtype[0].toUpperCase() + subtype.substring(1, subtype.length))
                    .join("");
            }
            return name + "PartViewer";
        },
        cleanPartsContent(messageKey) {
            this.parts = [];
            this.REMOVE_MESSAGE_PART_CONTENTS(messageKey);
        },
        getPartContent(part, index) {
            if (MimeType.isHtml(part)) {
                return this.htmlWithBlobs[index];
            }
            return this.message.partContentByAddress[part.address];
        }
    }
};
</script>

<style lang="scss">
.parts-viewer {
    .part-separator {
        margin: 1rem 0;
        border: 0;
        border-top: 1px solid rgba(0, 0, 0, 0.3);
        height: 0;
    }
}
</style>
