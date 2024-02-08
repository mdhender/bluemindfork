<template>
    <bm-toolbar class="mail-viewer-draft-toolbar float-right mail-viewer-mobile-actions bg-surface position-sticky">
        <mail-open-in-popup-with-shift v-slot="action" :href="route">
            <bm-toolbar-icon-button
                variant="regular-accent"
                size="sm"
                :title="action.label($t('mail.actions.edit'))"
                :icon="action.icon('pencil')"
                @click="action.execute(openEditor)"
            />
        </mail-open-in-popup-with-shift>
        <bm-toolbar-icon-button
            variant="regular-accent"
            size="sm"
            :title="$t('mail.actions.remove')"
            icon="trash"
            @click.stop="REMOVE_DRAFT(message, conversation)"
        />
    </bm-toolbar>
</template>

<script>
import { BmToolbar, BmToolbarIconButton } from "@bluemind/ui-components";
import { DraftMixin, RemoveMixin } from "~/mixins";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";

export default {
    name: "MailViewerDraftToolbar",
    components: { BmToolbar, BmToolbarIconButton, MailOpenInPopupWithShift },
    mixins: [DraftMixin, RemoveMixin],
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
@import "~@bluemind/ui-components/src/css/utils/variables";
.mail-viewer-mobile-actions {
    bottom: 0;
    justify-content: space-evenly;
}
</style>
