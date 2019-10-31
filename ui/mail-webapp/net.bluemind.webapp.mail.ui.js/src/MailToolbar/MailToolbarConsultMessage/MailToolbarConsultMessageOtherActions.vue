<template>
    <div class="d-inline-block h-100">
        <bm-dropdown 
            v-bm-tooltip.bottom.d500
            :no-caret="true"
            variant="link"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            class="other_actions h-100"
        >
            <template slot="button-content">
                <bm-icon icon="3dots" size="2x" /> {{ $tc("mail.toolbar.more") }}
            </template>
            <bm-dropdown-item class="shadow-sm" @click="deletionConfirmed">
                <div class="py-1">
                    <span class="font-weight-bold">{{ $t("mail.actions.purge") }}</span>
                    <span class="shortcuts float-right"> {{ $t("mail.shortcuts.purge") }}</span>
                </div>
            </bm-dropdown-item>
        </bm-dropdown>
    </div>
</template>

<script>
import { BmDropdown, BmDropdownItem, BmIcon, BmTooltip }  from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";

export default {
    name: "MailToolbarConsultMessageOtherActions",
    components: {
        BmDropdown,
        BmDropdownItem,
        BmIcon
    },
    directives: {BmTooltip},
    computed: {
        ...mapState("mail-webapp", ["currentFolderUid", "currentMessageId"]),
        ...mapGetters("mail-webapp", ["nextMessageId"])
    },
    methods: {
        ...mapActions("mail-webapp", ["purge"]),
        deletionConfirmed() {
            this.$router.push("" + (this.nextMessageId || ""));
            this.purge({ messageId: this.currentMessageId, folderUid: this.currentFolderUid });
        }
    }
};
</script>

<style lang="scss">
@import '~@bluemind/styleguide/css/_variables';

.other_actions .dropdown-menu {
    border: none !important;
    margin-top: map-get($spacers, 1) !important;
    padding: 0 !important;
    min-width: 20vw;
}
</style>