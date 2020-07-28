<template>
    <div class="h-100">
        <folder-result-content-empty v-if="count === 0" />
        <message-list v-if="count > 0" class="h-100" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContentEmpty from "./FolderResultContentEmpty";
import MessageList from "./MessageList";

export default {
    components: {
        FolderResultContentEmpty,
        MessageList
    },
    computed: {
        ...mapGetters("mail-webapp/messages", ["count"])
    }
};
</script>

<style></style>
