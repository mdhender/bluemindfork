<template>
    <mail-message-list-empty :image="emptyFolderIllustrationUnreadFilter">
        <h3 class="d-inline text-center">
            <p>{{ $t("mail.list.unread.none") }}</p>
            <p>
                <router-link :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)">
                    {{ $t("mail.list.filter.remove") }}
                </router-link>
            </p>
        </h3>
    </mail-message-list-empty>
</template>

<script>
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
    }
};
</script>
