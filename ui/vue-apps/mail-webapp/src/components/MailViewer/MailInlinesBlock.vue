<template>
    <div class="mail-inlines-block">
        <div v-for="(part, index) in inlines.html" :key="part.address">
            <hr v-if="index !== 0" class="part-separator" />
            <file-viewer-facade :message="message" :file="part" :related-parts="inlines.related">
                <template v-for="(_, slot) of $scopedSlots" #[slot]="scope">
                    <slot :name="slot" v-bind="scope" />
                </template>
            </file-viewer-facade>
        </div>
    </div>
</template>

<script>
import { partUtils } from "@bluemind/mail";
import { InlineImageHelper, MimeType } from "@bluemind/email";
import FileViewerFacade from "./FilesViewer/FileViewerFacade";
const { isViewable } = partUtils;

export default {
    name: "MailInlinesBlock",
    components: { FileViewerFacade },
    props: {
        message: {
            type: Object,
            required: true
        },
        parts: {
            type: Array,
            required: true
        }
    },
    computed: {
        inlines() {
            const contents = this.$store.state.mail.partsData.partsByMessageKey[this.message.key] || [];
            const cids = new CidSet(
                this.parts.flatMap(({ address, mime }) =>
                    MimeType.isHtml({ mime }) && contents[address] ? InlineImageHelper.cids(contents[address]) : []
                )
            );
            const html = [];
            const related = [];
            this.parts.forEach(part => {
                if (isViewable(part) && !(MimeType.isImage(part) && cids.has(part.contentId))) {
                    html.push(part);
                } else {
                    related.push(part);
                }
            });
            return { html, related };
        }
    }
};
class CidSet extends Set {
    has(cid) {
        const r = /^<?([^>]*)>?$/;
        return cid && super.has(cid.replace(r, "$1").toUpperCase());
    }
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-inlines-block {
    padding-top: $sp-6;
    padding-bottom: $sp-6;

    .part-separator {
        margin: 1rem 0;
        border: 0;
        border-top: 1px solid $neutral-fg-lo2;
        height: 0;
    }
}
</style>
