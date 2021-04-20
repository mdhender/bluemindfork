<template>
    <bm-dropdown
        :no-caret="true"
        variant="inline-light"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        toggle-class="btn-lg-simple-dark"
        class="other_actions h-100"
        right
    >
        <template slot="button-content">
            <bm-icon icon="3dots" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.toolbar.more") }}</span>
        </template>
        <bm-dropdown-item class="shadow-sm" :shortcut="$t('mail.shortcuts.purge')" @click="REMOVE_MESSAGES(selected)">
            {{ $t("mail.actions.purge") }}
        </bm-dropdown-item>
    </bm-dropdown>
</template>

<script>
import { BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { RemoveMixin } from "~mixins";
import { ACTIVE_MESSAGE } from "~getters";

export default {
    name: "MailToolbarConsultMessageOtherActions",
    components: {
        BmDropdown,
        BmDropdownItem,
        BmIcon
    },
    mixins: [RemoveMixin],
    computed: {
        ...mapState("mail", ["selection", "messages"]),
        ...mapGetters("mail", { ACTIVE_MESSAGE }),
        selected() {
            return this.selection.length ? this.selection.map(key => this.messages[key]) : this.ACTIVE_MESSAGE;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.other_actions .dropdown-menu {
    border: none !important;
    margin-top: $sp-1 !important;
    padding: 0 !important;
}
</style>
