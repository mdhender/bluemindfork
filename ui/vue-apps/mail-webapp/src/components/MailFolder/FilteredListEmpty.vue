<template>
    <div class="filtered-list-empty d-flex flex-column align-items-center justify-content-around h-50">
        <div class="h-50">
            <img class="" :src="folderFilterNoResults" :alt="$t('mail.folder.filter.no_results')" />
        </div>
        <bm-button variant="text" @click="$emit('clearFilter')">
            {{ $t("mail.folder.filter.remove") }}
        </bm-button>
    </div>
</template>

<script>
import folderFilterNoResults from "../../../assets/folder-filter-no-results.png";
import { BmButton } from "@bluemind/ui-components";

export default {
    name: "FilteredListEmpty",
    components: { BmButton },
    data() {
        return { folderFilterNoResults };
    }
};
</script>
