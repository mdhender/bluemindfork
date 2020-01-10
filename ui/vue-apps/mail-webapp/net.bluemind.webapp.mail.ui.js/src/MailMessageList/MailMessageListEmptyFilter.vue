<template>
    <mail-message-list-empty :image="image_">
        <h3 class="d-inline text-center">
            <p>{{ $t("mail.list.unread.none") }}</p>
            <p>
                <router-link :to="{ path: '', query: { filter: undefined }}">
                    {{ $t("mail.list.filter.remove") }}
                </router-link>
            </p>
        </h3>
    </mail-message-list-empty>
</template>

<script>
import { mapState } from "vuex";
import emptyFolderIllustrationUnreadFilter from "../../assets/empty-folder-unread-filter.png";
import MailMessageListEmpty from "./MailMessageListEmpty";

export default {
    name: "MailMessageListEmptyFolder",
    components: {
        MailMessageListEmpty
    },
    data() {
        return {
            emptyFolderIllustrationUnreadFilter
        };
    },
    computed: {
        ...mapState("mail-webapp", ["messageFilter"]),
        image_() {
            switch (this.messageFilter) {
                case "unread":
                    return this.emptyFolderIllustrationUnreadFilter;
                case "all":
                default:
                    return null;
            }
        }
    }
};
</script>
