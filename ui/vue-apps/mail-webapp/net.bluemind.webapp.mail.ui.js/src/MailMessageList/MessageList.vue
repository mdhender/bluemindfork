<template>
    <bm-list-group
        class="message-list"
        tabindex="0"
        @scroll="onScroll"
        @keyup.shift.delete.exact.prevent="openPurgeModal"
        @keyup.delete.exact.prevent="remove"
        @keyup.up="goToByDiff(-1)"
        @keyup.down="goToByDiff(+1)"
        @keyup.page-down="goToByDiff(+PAGE)"
        @keyup.page-up="goToByDiff(-PAGE)"
        @keyup.home="goTo(0)"
        @keyup.end="goTo(length - 1)"
    >
        <div v-for="(message, index) in messages" :key="index">
            <message-list-separator v-if="message.hasSeparator" :text="$t(message.range.name)" />
            <message-list-item
                :ref="'message-' + message.key"
                :message="message"
                :to="messageRoute(message.key)"
                class="message-list-item"
            />
        </div>
        <bm-list-group-item v-if="hasMore">Loading…</bm-list-group-item>
    </bm-list-group>
</template>

<script>
import { BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
import { mapState, mapGetters, mapActions } from "vuex";
import { SHOW_PURGE_MODAL } from "../VueBusEventTypes";
import MailRouterMixin from "../MailRouterMixin";
import MessageListItem from "./MessageListItem";
import MessageListSeparator from "./MessageListSeparator";
import throttle from "lodash.throttle";

const PAGE = 9;

export default {
    name: "MessageList",
    components: {
        BmListGroup,
        BmListGroupItem,
        MessageListItem,
        MessageListSeparator
    },
    mixins: [MailRouterMixin],
    data() {
        return {
            PAGE
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentMessageKey", "currentFolderKey"]),
        ...mapGetters("mail-webapp/messages", ["messages", "count"]),
        hasMore: function() {
            return this.messages.length < this.count;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["loadRange"]),
        messageRoute(key = "") {
            const path = this.$route.path;
            const filter = this.areMessagesFiltered ? "?filter=" + this.messageFilter : "";
            if (this.$route.params.mail) {
                return path.replace(new RegExp("/" + this.$route.params.mail + "/?.*"), "/" + key) + filter;
            } else if (path === "/mail/" || path === "/mail/new") {
                return "/mail/" + this.currentFolderKey + "/" + key + filter;
            }
            return path + key + filter;
        },
        loadMore: function() {
            const range = {
                start: this.messages.length,
                end: this.messages.length + 100
            };
            this.loadRange(range);
        },
        onScroll: throttle(function(event) {
            const total = event.target.scrollHeight;
            const current = event.target.scrollTop;
            if (current >= 0.75 * total) {
                this.loadMore();
            }
        }, 300),
        goToByDiff(diff) {
            if (this.currentMessageKey) {
                let index = this.indexOf(this.currentMessageKey) + diff;
                this.goTo(index);
            }
        },
        goTo(index) {
            // FIXME
            this.$router.push({ path: index });
        },
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.openPurgeModal();
                return;
            }
            this.$router.push(this.computeMessageRoute(this.currentFolderKey, this.nextMessageKey, this.messageFilter));
            this.$store.dispatch("mail-webapp/remove", this.currentMessageKey);
        },
        openPurgeModal() {
            this.$bus.$emit(SHOW_PURGE_MODAL);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.message-list {
    overflow-y: auto;
}

.message-list-item {
    cursor: pointer;
}
</style>
