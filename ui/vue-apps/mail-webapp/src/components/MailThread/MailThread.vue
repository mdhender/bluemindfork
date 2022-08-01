<template>
    <div class="mail-thread d-flex flex-column">
        <bm-alert-area class="position-sticky sticky-top" :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <mail-conversation-viewer :conversation="CURRENT_CONVERSATION_METADATA" />
        <mail-attachment-preview />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";

import { INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/styleguide";

import { CURRENT_CONVERSATION_METADATA } from "~/getters";
import MailConversationViewer from "../MailViewer/MailConversationViewer";
import MailAttachmentPreview from "../MailAttachment/MailAttachmentPreview";

export default {
    name: "MailThread",
    components: { BmAlertArea, MailConversationViewer, MailAttachmentPreview },
    computed: {
        ...mapState("mail", ["folders"]),
        ...mapGetters("mail", { CURRENT_CONVERSATION_METADATA }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "right-panel") }),
        folder() {
            return this.CURRENT_CONVERSATION_METADATA && this.folders[this.CURRENT_CONVERSATION_METADATA.folderRef.key];
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "right-panel", renderer: "DefaultAlert" }
            };
        }
    },
    watch: {
        "folder.key": {
            handler() {
                if (this.folder && !this.folder.writable) {
                    this.INFO(this.readOnlyAlert);
                } else {
                    this.REMOVE(this.readOnlyAlert.alert);
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("alert", { REMOVE, INFO })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread {
    .mail-composer ~ .mail-viewer {
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            display: none !important;
        }
    }

    .mail-composer {
        @media (min-width: map-get($grid-breakpoints, "lg")) {
            height: auto !important;
        }
    }
}
</style>
