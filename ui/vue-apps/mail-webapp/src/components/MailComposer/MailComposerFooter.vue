<template>
    <div class="mail-composer-footer">
        <bm-alert-area v-if="alerts.length > 0" :alerts="alerts" class="w-100">
            <template v-slot="slotProps">
                <component :is="slotProps.alert.renderer" :alert="slotProps.alert" />
            </template>
        </bm-alert-area>
        <transition name="slide-fade">
            <bm-rich-editor-toolbar
                v-if="showTextFormattingToolbar"
                align="right"
                editor="composer"
                :default-font-family="defaultFont"
                :extra-font-families="extraFontsFamilies"
            />
        </transition>
        <mail-composer-toolbar
            :message="message"
            :is-signature-inserted="isSignatureInserted"
            :is-delivery-status-requested.sync="isDeliveryStatusRequested"
            :is-disposition-notification-requested.sync="isDispositionNotificationRequested"
            @toggle-signature="$emit('toggle-signature')"
            @toggle-delivery-status="$emit('toggle-delivery-status')"
            @toggle-disposition-notification="$emit('toggle-disposition-notification')"
        />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmAlertArea, BmRichEditorToolbar } from "@bluemind/ui-components";
import MailComposerToolbar from "./MailComposerToolbar";

export default {
    name: "MailComposerFooter",
    components: { BmAlertArea, BmRichEditorToolbar, MailComposerToolbar },
    props: {
        message: { type: Object, required: true },
        isSignatureInserted: { type: Boolean, required: true },
        isDeliveryStatusRequested: { type: Boolean, required: true },
        isDispositionNotificationRequested: { type: Boolean, required: true }
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "composer-footer") }),
        showTextFormattingToolbar() {
            return this.$store.state.mail.messageCompose.showFormattingToolbar;
        },
        defaultFont() {
            return this.$store.state.settings.composer_default_font;
        },
        extraFontsFamilies() {
            return this.$store.getters["settings/EXTRA_FONT_FAMILIES"];
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

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
    .bm-alert-area .bm-alert {
        padding-top: $sp-5;
        padding-bottom: $sp-5;
        margin-bottom: 0;
    }
}
</style>
