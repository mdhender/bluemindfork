<template>
    <bm-button-toolbar class="message-list-item-quick-action-buttons">
        <bm-button-group>
            <bm-button
                v-if="folderOfMessage.writable"
                v-bm-tooltip.top.viewport
                :aria-label="$tc('mail.actions.remove.aria')"
                :title="$tc('mail.actions.remove.aria')"
                class="p-1 mr-2"
                variant="inline-secondary"
                @click.shift.exact.prevent.stop="$emit('purge')"
                @click.exact.prevent.stop="remove"
            >
                <bm-icon icon="trash" size="lg" />
            </bm-button>
            <bm-button
                v-if="!message.flags.includes(Flag.SEEN)"
                v-bm-tooltip.top.viewport
                class="p-1"
                :aria-label="$tc('mail.actions.mark_read.aria')"
                :title="$tc('mail.actions.mark_read.aria')"
                variant="inline-secondary"
                @click.prevent.stop="markAsRead([message.key])"
            >
                <bm-icon icon="read" size="lg" />
            </bm-button>
            <bm-button
                v-else
                v-bm-tooltip.top.viewport
                class="p-1"
                :aria-label="$tc('mail.actions.mark_unread.aria')"
                :title="$tc('mail.actions.mark_unread.aria')"
                variant="inline-secondary"
                @click.prevent.stop="markAsUnread([message.key])"
            >
                <bm-icon icon="unread" size="lg" />
            </bm-button>
            <template v-if="folderOfMessage.writable">
                <bm-button
                    v-if="!message.flags.includes(Flag.FLAGGED)"
                    v-bm-tooltip.top.viewport
                    class="p-1 ml-2"
                    :aria-label="$tc('mail.actions.mark_flagged.aria')"
                    :title="$tc('mail.actions.mark_flagged.aria')"
                    variant="inline-secondary"
                    @click.prevent.stop="markAsFlagged([message.key])"
                >
                    <bm-icon icon="flag-outline" size="lg" />
                </bm-button>
                <bm-button
                    v-else
                    v-bm-tooltip.top.viewport
                    class="p-1 ml-2"
                    :aria-label="$tc('mail.actions.mark_unflagged.aria')"
                    :title="$tc('mail.actions.mark_unflagged.aria')"
                    variant="inline-secondary"
                    @click.prevent.stop="markAsUnflagged([message.key])"
                >
                    <bm-icon class="text-warning" icon="flag-fill" size="lg" />
                </bm-button>
            </template>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { Flag } from "@bluemind/email";
import { MY_TRASH } from "~getters";
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
    data() {
        return {
            Flag
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageKey"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail", { MY_TRASH }),
        folderOfMessage() {
            return this.folders[this.message.folderRef.key];
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread", "markAsFlagged", "markAsUnflagged"]),
        remove() {
            if (this.activeFolder === this.MY_TRASH.key) {
                this.$emit("purge");
                return;
            }
            // do this before followed async operations
            const nextMessageKey = this.nextMessageKey;
            this.$store.dispatch("mail-webapp/remove", this.message.key);
            if (this.currentMessageKey === this.message.key) {
                this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
            }
        }
    }
};
</script>
<style lang="scss">
.message-list-item-quick-action-buttons {
    .hovershadow:hover {
        box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
    }
}
</style>
