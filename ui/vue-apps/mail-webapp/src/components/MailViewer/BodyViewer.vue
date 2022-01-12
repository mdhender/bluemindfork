<template>
    <div class="body-viewer">
        <mail-attachments-block :attachments="attachments" :message="message" />
        <event-viewer v-if="message.hasICS && currentEvent" :parts="inlines" :message="message" />
        <mail-inlines-block v-else :message="message" :parts="inlines">
            <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
                <slot :name="slot" v-bind="scope" />
            </template>
        </mail-inlines-block>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { MimeType, InlineImageHelper } from "@bluemind/email";
import { hasRemoteImages } from "@bluemind/html-utils";
import { WARNING } from "@bluemind/alert.store";

import { create as createAttachment, AttachmentStatus } from "~/model/attachment";
import { VIEWER_CAPABILITIES, getPartsFromCapabilities } from "~/model/part";
import { COMPUTE_QUOTE_NODES, FETCH_PART_DATA, SET_BLOCK_REMOTE_IMAGES } from "~/actions";
import { CONVERSATION_MESSAGE_BY_KEY } from "~/getters";
import apiAddressbooks from "~/store/api/apiAddressbooks";

import MailInlinesBlock from "./MailInlinesBlock";
import EventViewer from "./EventViewer";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";

export default {
    name: "BodyViewer",
    components: { EventViewer, MailAttachmentsBlock, MailInlinesBlock },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            quotedCollapsed: true,
            alert: {
                alert: { name: "mail.BLOCK_REMOTE_CONTENT", uid: "BLOCK_REMOTE_CONTENT", payload: this.message },
                options: { area: "right-panel", renderer: "BlockedRemoteContent" }
            }
        };
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapGetters("mail", { CONVERSATION_MESSAGE_BY_KEY }),
        trustRemoteContent() {
            return this.$store.state.session.settings.remote.trust_every_remote_content !== "false";
        },
        contents() {
            return this.$store.state.mail.partsData.partsByMessageKey[this.message.key];
        },
        inlines() {
            let contents = this.$store.state.mail.partsData.partsByMessageKey[this.message.key] || [];
            const cids = new CidSet(
                this.parts.flatMap(({ address, mime }) =>
                    MimeType.isHtml({ mime }) && contents[address] ? InlineImageHelper.cids(contents[address]) : []
                )
            );
            return this.parts.filter(
                part => isDisplayable(part) && !(MimeType.isImage(part) && cids.has(part.contentId))
            );
        },
        parts() {
            return getPartsFromCapabilities(this.message, VIEWER_CAPABILITIES);
        },
        attachments() {
            const fallback = this.parts
                .filter(part => !isDisplayable(part))
                .map(part => createAttachment(part, AttachmentStatus.ONLY_LOCAL));
            return [...this.message.attachments, ...fallback];
        }
    },
    async created() {
        this.SET_BLOCK_REMOTE_IMAGES(true);
        const texts = this.parts.filter(part => MimeType.isHtml(part) || MimeType.isText(part));
        await this.FETCH_PART_DATA({
            messageKey: this.message.key,
            folderUid: this.message.folderRef.uid,
            imapUid: this.message.remoteRef.imapUid,
            inlines: texts
        });
        const conversationMessages = this.message.conversationRef
            ? this.CONVERSATION_MESSAGE_BY_KEY(this.message.conversationRef.key)
            : [this.message];
        this.COMPUTE_QUOTE_NODES({ message: this.message, conversationMessages });
        if (!this.trustRemoteContent) {
            const hasImages = texts.some(part => MimeType.isHtml(part) && hasRemoteImages(this.contents[part.address]));
            if (hasImages) {
                const { total } = await apiAddressbooks.search(this.message.from.address);
                if (total === 0) {
                    this.WARNING(this.alert);
                } else {
                    this.SET_BLOCK_REMOTE_IMAGES(false);
                }
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_PART_DATA, COMPUTE_QUOTE_NODES }),
        ...mapActions("alert", { WARNING }),
        ...mapMutations("mail", { SET_BLOCK_REMOTE_IMAGES })
    }
};

function isDisplayable({ mime }) {
    return VIEWER_CAPABILITIES.some(available => mime.startsWith(available));
}

class CidSet extends Set {
    has(cid) {
        const r = /^<?([^>]*)>?$/;
        return cid && super.has(cid.replace(r, "$1").toUpperCase());
    }
}
</script>

<style lang="scss">
.body-viewer {
    display: flex;
    flex-direction: column;
}
</style>
