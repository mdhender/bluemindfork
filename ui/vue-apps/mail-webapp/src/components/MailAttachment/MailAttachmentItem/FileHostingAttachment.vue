<template>
    <div class="file-hosting-attachment">
        <attachment-preview v-if="!compact" :attachment="fileHosted" :message="message" />
        <attachment-infos :attachment="fileHosted" :message="message">
            <template #actions>
                <slot name="actions" v-bind="{ attachment }" />
            </template>
            <template v-slot:subtitle="scope"> <bm-icon icon="cloud" class="mr-1" />{{ scope.size }} </template>
        </attachment-infos>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";
import AttachmentInfos from "./AttachmentInfos";
import AttachmentPreview from "./AttachmentPreview";
import { mapGetters } from "vuex";

export default {
    name: "FileHostingAttachment",
    components: {
        AttachmentPreview,
        AttachmentInfos,
        BmIcon
    },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        },
        compact: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        fileHosted() {
            return {
                ...this.attachment,
                ...this.$store.state.mail.filehosting.values[this.message.key][this.attachment.address]
            };
        }
    },
    methods: {
        ...mapGetters("mail", ["GET_FH_ATTACHMENT"])
    },
    handle({ headers }) {
        return (
            headers.find(header => header.name.toLowerCase() === "x-bm-disposition") ||
            headers.find(header => header.name.toLowerCase() === "x-mozilla-cloud-part")
        );
    }
};
</script>
