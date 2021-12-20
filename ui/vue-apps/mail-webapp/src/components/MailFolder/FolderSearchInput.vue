<template>
    <bm-form-input
        v-model="folderSearchPattern"
        class="folder-search-input"
        icon="filter"
        resettable
        left-icon
        :placeholder="$t('mail.folder.search.placeholder')"
        @reset="folderSearchPattern = ''"
    />
</template>

<script>
import { BmFormInput } from "@bluemind/styleguide";

export default {
    name: "FolderSearchInput",
    components: { BmFormInput },
    data() {
        return { folderSearchPattern: "" };
    }
};
</script>
