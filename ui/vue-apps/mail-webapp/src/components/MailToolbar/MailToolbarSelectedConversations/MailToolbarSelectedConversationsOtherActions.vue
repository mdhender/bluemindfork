<template>
    <bm-dropdown
        :no-caret="true"
        variant="inline-light"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        toggle-class="btn-lg-simple-dark"
        class="mail-toolbar-consult-message-other-actions h-100"
        right
    >
        <template slot="button-content">
            <bm-icon icon="3dots" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.toolbar.more") }}</span>
        </template>
        <bm-dropdown-item
            class="shadow-sm"
            :shortcut="$t('mail.shortcuts.purge')"
            :title="removeAriaText(subject)"
            :aria-label="removeAriaText(subject)"
            @click="remove()"
        >
            {{ $t("mail.actions.purge") }}
        </bm-dropdown-item>
    </bm-dropdown>
</template>

<script>
import { BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";
import { ActionTextMixin, RemoveMixin } from "~/mixins";

export default {
    name: "MailToolbarConsultMessageOtherActions",
    components: { BmDropdown, BmDropdownItem, BmIcon },
    mixins: [ActionTextMixin, RemoveMixin],
    props: {
        subject: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-consult-message-other-actions .dropdown-menu {
    border: none !important;
    margin-top: $sp-1 !important;
    padding: 0 !important;
}
</style>
