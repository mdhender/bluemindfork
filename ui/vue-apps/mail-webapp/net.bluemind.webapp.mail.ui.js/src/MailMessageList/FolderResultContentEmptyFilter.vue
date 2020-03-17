<template>
    <mail-message-list-empty :image="emptyFolderIllustrationUnreadFilter">
        <h3 class="d-inline text-center">
            <p>{{ $t("mail.list.unread.none") }}</p>
            <p>
                <router-link :to="{ name: 'v:mail:home', params: { filter: null } }">
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
    name: "FolderResultContentEmptyFilter",
    components: {
        MailMessageListEmpty
    },
    data() {
        return {
            emptyFolderIllustrationUnreadFilter
        };
    },
    computed: {
        ...mapState("mail-webapp", ["messageFilter"])
    }
};
</script>
