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
import debounce from "lodash.debounce";
import { BmFormInput } from "@bluemind/styleguide";
import { SET_FOLDER_FILTER_LOADED, SET_FOLDER_FILTER_LOADING, SET_FOLDER_FILTER_PATTERN } from "~/mutations";
import { mapMutations, mapState } from "vuex";

const SEARCH_DELAY = 500;

export default {
    name: "FolderSearchInput",
    components: { BmFormInput },
    data() {
        return {
            folderSearchPattern_: "",
            debouncedSetPattern: debounce(() => {
                this.SET_FOLDER_FILTER_PATTERN(this.folderSearchPattern_);
                this.SET_FOLDER_FILTER_LOADED();
            }, SEARCH_DELAY)
        };
    },
    computed: {
        ...mapState("mail", ["folderList"]),
        folderSearchPattern: {
            get() {
                return this.folderList.pattern;
            },
            set(value) {
                this.SET_FOLDER_FILTER_LOADING();
                this.folderSearchPattern_ = value;
                if (value?.trim()) {
                    this.debouncedSetPattern();
                } else {
                    this.SET_FOLDER_FILTER_PATTERN(null); // TODO handle reset here or in store, modify FilteredLitEmpty accordingly
                    this.SET_FOLDER_FILTER_LOADED();
                }
            }
        }
    },
    methods: {
        ...mapMutations("mail", { SET_FOLDER_FILTER_LOADED, SET_FOLDER_FILTER_LOADING, SET_FOLDER_FILTER_PATTERN })
    }
};
</script>
