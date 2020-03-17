<template>
    <div class="d-inline-block h-100">
        <bm-dropdown
            v-bm-tooltip.bottom.ds500
            :no-caret="true"
            variant="link"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            class="other_actions h-100"
            right
        >
            <template slot="button-content">
                <bm-icon icon="3dots" size="2x" />
                <span class="d-none d-lg-block">{{ $tc("mail.toolbar.more") }}</span>
            </template>
            <bm-dropdown-item class="shadow-sm" :data-shortcut="$t('mail.shortcuts.purge')" @click="deletionConfirmed">
                {{ $t("mail.actions.purge") }}
            </bm-dropdown-item>
        </bm-dropdown>
    </div>
</template>

<script>
import { BmDropdown, BmDropdownItem, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";

export default {
    name: "MailToolbarConsultMessageOtherActions",
    components: {
        BmDropdown,
        BmDropdownItem,
        BmIcon
    },
    directives: { BmTooltip },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail-webapp", ["nextMessageKey"])
    },
    methods: {
        ...mapActions("mail-webapp", ["purge"]),
        deletionConfirmed() {
            this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
            this.purge(this.currentMessageKey);
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
