<template>
    <div class="mail-composer-footer">
        <transition name="slide-fade">
            <bm-rich-editor-toolbar v-if="showTextFormattingToolbar" align="right" editor="composer" />
        </transition>
        <mail-composer-toolbar
            :message="message"
            :is-signature-inserted="isSignatureInserted"
            @toggle-signature="$emit('toggle-signature')"
        />
    </div>
</template>

<script>
import { BmRichEditorToolbar } from "@bluemind/styleguide";
import { AppDataKeys } from "@bluemind/webappdata";

import MailComposerToolbar from "./MailComposerToolbar";

export default {
    name: "MailComposerFooter",
    components: {
        MailComposerToolbar,
        BmRichEditorToolbar
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        isSignatureInserted: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        showTextFormattingToolbar() {
            const appData = this.$store.state["root-app"].appData[AppDataKeys.MAIL_COMPOSITION_SHOW_FORMATTING_TOOLBAR];
            return appData ? appData.value : false;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-footer {
    background-color: $surface;
    .bm-rich-editor-toolbar.full-toolbar {
        border-top-color: $neutral-fg-lo3;
        border-width: 1px 0 0 0;
    }
    .slide-fade-enter-active,
    .slide-fade-leave-active {
        transition: all 0.1s ease-out;
    }
    .slide-fade-enter,
    .slide-fade-leave-to {
        transform: translateY(20px);
    }
}
</style>
