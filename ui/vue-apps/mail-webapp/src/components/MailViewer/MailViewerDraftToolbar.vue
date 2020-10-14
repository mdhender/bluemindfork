<template>
    <bm-button-toolbar
        key-nav
        class="mail-viewer-draft-toolbar float-right mail-viewer-mobile-actions bg-white position-sticky"
    >
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.actions.edit')"
            :title="$t('mail.actions.edit')"
            @click="SET_MESSAGE_COMPOSING({ messageKey: message.key, composing: true })"
        >
            <bm-icon icon="pencil" size="lg" />
            <span class="d-lg-none">{{ $t("mail.actions.edit") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.actions.remove')"
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
import { ComposerInitMixin, RemoveMixin } from "~/mixins";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import { mapMutations } from "vuex";

export default {
    name: "MailViewerDraftToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    mixins: [ComposerInitMixin, RemoveMixin],
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
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_COMPOSING })
    }
};
</script>
<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/_variables";
@media (max-width: map-get($grid-breakpoints, "lg")) {
    .mail-viewer-mobile-actions {
        bottom: 0;
        box-shadow: 0 -0.125rem 0.125rem rgba($dark, 0.25);
        justify-content: space-evenly;
    }
}
</style>
