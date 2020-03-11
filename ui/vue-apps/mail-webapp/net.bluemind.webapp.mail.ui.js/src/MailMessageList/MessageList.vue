<template>
    <bm-list-group
        class="message-list"
        @scroll="onScroll"
        @keyup.up="moveUp()"
        @keyup.down="moveDown()"
        @keyup.home="moveStart()"
        @keyup.end="moveEnd()"
        @keyup.page-up="movePageUp()"
        @keyup.page-down="movePageDown()"
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
import { mapState, mapGetters, mapActions } from "vuex";
import { BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
import throttle from "lodash.throttle";
import MessageListItem from "./MessageListItem";
import MessageListSeparator from "./MessageListSeparator";

const PAGE = 9;
function getCurrentMessageIndex(messages, key) {
    return messages.map(message => message.key).indexOf(key);
}

export default {
    name: "MessageList",
    components: {
        BmListGroup,
        BmListGroupItem,
        MessageListItem,
        MessageListSeparator
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
        moveUp() {
            moveTo(getCurrentMessageIndex(this.messages, this.currentMessageKey) - 1);
        },
        moveDown() {
            moveTo(getCurrentMessageIndex(this.messages, this.currentMessageKey) + 1);
        },
        moveEnd() {
            moveTo(this.messages.length - 1);
        },
        moveStart() {
            moveTo(0);
        },
        movePageUp() {
            moveTo(getCurrentMessageIndex(this.messages, this.currentMessageKey) - PAGE);
        },
        movePageDown() {
            moveTo(getCurrentMessageIndex(this.messages, this.currentMessageKey) + PAGE);
        },
        moveTo(index) {
            this.goTo(this.messages[index].key);
        },
        goTo(key) {
            this.$router.push({ path: key });
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
