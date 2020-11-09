<template>
    <div class="h-100">
        <folder-result-content-empty v-if="MESSAGE_LIST_COUNT === 0" />
        <message-list v-if="MESSAGE_LIST_COUNT > 0" class="h-100" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContentEmpty from "./FolderResultContentEmpty";
import MessageList from "./MessageList";
import { MESSAGE_LIST_COUNT } from "~getters";

export default {
    components: {
        FolderResultContentEmpty,
        MessageList
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_LIST_COUNT })
    }
};
</script>

<style></style>
