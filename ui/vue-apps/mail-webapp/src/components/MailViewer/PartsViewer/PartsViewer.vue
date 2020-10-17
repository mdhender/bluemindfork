<template>
    <div class="parts-viewer py-2">
        <template v-for="(part, index) in parts">
            <template v-if="isSupportedPart(part)">
                <hr v-if="index !== 0" :key="part.address + '-sepatator'" class="part-separator" />
                <component
                    :is="computePartComponent(part.mime)"
                    :key="part.address"
                    :value="message.partContentByAddress[part.address]"
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
import ImagePartViewer from "./ImagePartViewer";
import TextHtmlPartViewer from "./TextHtmlPartViewer";
import TextPlainPartViewer from "./TextPlainPartViewer";
import mutationTypes from "../../../store/mutationTypes";

const CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

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
            parts: []
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

                const service = inject("MailboxItemsPersistence", this.message.folderRef.uid);
                const inlines = this.message.inlinePartsByCapabilities.find(part =>
                    part.capabilities.every(capability => CAPABILITIES.includes(capability))
                ).parts;

                const contents = await fetchAll(this.message.remoteRef.imapUid, service, inlines, false);
                this.SET_MESSAGE_PART_CONTENTS({ key: this.message.key, contents, parts: inlines });

                const html = inlines.filter(part => part.mime === MimeType.TEXT_HTML);
                const images = inlines.filter(part => MimeType.isImage(part) && part.contentId);
                const inlined = InlineImageHelper.insertInlineImages(html, images, this.message.partContentByAddress)
                    .inlined;
                const others = inlines.filter(
                    part => part.mime !== MimeType.TEXT_HTML && !inlined.includes(part.contentId)
                );
                this.parts = [...html, ...others];
            },
            immediate: true
        }
    },
    destroyed() {
        this.cleanPartsContent(this.messageKey);
    },
    methods: {
        ...mapMutations("mail", [mutationTypes.SET_MESSAGE_PART_CONTENTS, mutationTypes.REMOVE_MESSAGE_PART_CONTENTS]),
        isHtmlPart(part) {
            return MimeType.isHtml(part);
        },
        isTextPart(part) {
            return MimeType.isText(part);
        },
        isImagePart(part) {
            return MimeType.isImage(part);
        },
        isSupportedPart(part) {
            return this.isHtmlPart(part) || this.isTextPart(part) || this.isImagePart(part);
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
