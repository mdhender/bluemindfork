<template>
    <bm-button-toolbar class="message-list-item-quick-action-buttons justify-content-end">
        <bm-button-group>
            <bm-button
                v-if="!isReadOnlyFolder(folderUidOfMessage)"
                v-bm-tooltip.ds500.top.viewport
                :aria-label="$tc('mail.actions.remove.aria')"
                :title="$tc('mail.actions.remove.aria')"
                class="p-1 mr-2 border-0 hovershadow"
                variant="inline-secondary"
                @click.shift.exact.prevent.stop="purge"
                @click.exact.prevent.stop="remove"
            >
                <bm-icon icon="trash" size="sm" />
            </bm-button>
            <bm-button
                v-if="message.states.includes('not-seen')"
                v-bm-tooltip.ds500.top.viewport
                class="p-1 border-0 hovershadow"
                :aria-label="$tc('mail.actions.mark_read.aria')"
                :title="$tc('mail.actions.mark_read.aria')"
                variant="inline-secondary"
                @click.prevent.stop="markAsRead([message.key])"
            >
                <bm-icon icon="read" size="sm" />
            </bm-button>
            <bm-button
                v-else
                v-bm-tooltip.ds500.top.viewport
                class="p-1 border-0 hovershadow"
                :aria-label="$tc('mail.actions.mark_unread.aria')"
                :title="$tc('mail.actions.mark_unread.aria')"
                variant="inline-secondary"
                @click.prevent.stop="markAsUnread([message.key])"
            >
                <bm-icon icon="unread" size="sm" />
            </bm-button>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { mapActions, mapGetters, mapState } from "vuex";

export default {
    name: "MessageListItemQuickActionButtons",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageKey", "my", "isReadOnlyFolder"]),
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        folderUidOfMessage() {
            return ItemUri.container(this.message.key);
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.purge();
                return;
            }
            if (this.currentMessageKey === this.message.key) {
                this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
            }
            this.$store.dispatch("mail-webapp/remove", this.message.key);
        },
        async purge() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("mail.actions.purge.modal.content", { subject: this.message.subject }),
                {
                    title: this.$t("mail.actions.purge.modal.title"),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                if (this.currentMessageKey === this.message.key) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
                }
                this.$store.dispatch("mail-webapp/purge", this.message.key);
            }
        }
    }
};
</script>
<style>
.message-list-item-quick-action-buttons .hovershadow:hover {
    box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
}
</style>
