<template>
    <bm-button-toolbar
        key-nav
        class="mail-viewer-draft-toolbar float-right mail-viewer-mobile-actions bg-surface position-sticky"
    >
        <mail-open-in-popup-with-shift v-slot="action" :href="route">
            <bm-button
                variant="simple-secondary"
                :title="action.label($t('mail.actions.edit'))"
                @click="action.execute(openEditor)"
            >
                <bm-icon :icon="action.icon('pencil')" size="lg" />
                <span class="d-lg-none">{{ $t("mail.actions.edit") }}</span>
            </bm-button>
        </mail-open-in-popup-with-shift>
        <bm-button
            variant="simple-secondary"
            :title="$t('mail.actions.remove')"
            @click="REMOVE_DRAFT(conversation, message)"
        >
            <bm-icon icon="trash" size="lg" />
            <span class="d-lg-none">{{ $t("mail.actions.remove") }}</span>
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";
import { DraftMixin, ComposerInitMixin, RemoveMixin } from "~/mixins";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";

export default {
    name: "MailViewerDraftToolbar",
    components: { BmButton, BmButtonToolbar, BmIcon, MailOpenInPopupWithShift },
    mixins: [DraftMixin, ComposerInitMixin, RemoveMixin],
    props: {
        conversation: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        route() {
            return this.$router.relative({
                name: "mail:message",
                params: { messagepath: MessagePathParam.build("", this.message) }
            });
        }
    },
    methods: {
        async openEditor() {
            await this.saveAndCloseOpenDrafts(this.conversation);
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: true });
        }
    }
};
</script>
<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/_variables";
@media (max-width: map-get($grid-breakpoints, "lg")) {
    .mail-viewer-mobile-actions {
        bottom: 0;
        box-shadow: 0 -0.125rem 0.125rem rgba($highest, 0.25);
        justify-content: space-evenly;
    }
}
</style>
