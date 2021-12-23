<template>
    <div class="filtered-list-empty d-flex flex-column align-items-center justify-content-center text-center">
        <img class="d-block m-auto pb-2" :src="folderFilterNoResults" :alt="$t('mail.folder.filter.no_results')" />
        <bm-button variant="inline-secondary" @click="clearFilter">{{ $t("mail.folder.filter.remove") }}</bm-button>
    </div>
</template>

<script>
import folderFilterNoResults from "../../../assets/folder-filter-no-results.png";
import { BmButton } from "@bluemind/styleguide";
import { mapMutations } from "vuex";
import { SET_FOLDER_FILTER_PATTERN, SET_FOLDER_FILTER_RESULTS, RESET_FOLDER_FILTER_LIMITS } from "~/mutations";

export default {
    name: "FilteredListEmpty",
    components: { BmButton },
    data() {
        return {
            folderFilterNoResults
        };
    },
    methods: {
        ...mapMutations("mail", { SET_FOLDER_FILTER_PATTERN, SET_FOLDER_FILTER_RESULTS, RESET_FOLDER_FILTER_LIMITS }),
        clearFilter() {
            this.SET_FOLDER_FILTER_PATTERN(null);
            this.SET_FOLDER_FILTER_RESULTS({});
            this.RESET_FOLDER_FILTER_LIMITS();
        }
    }
};
</script>
