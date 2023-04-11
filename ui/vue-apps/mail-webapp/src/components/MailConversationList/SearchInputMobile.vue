<template>
    <!-- TODO i18n -->
    <div class="search-input-mobile">
        <bm-form-input
            ref="search"
            v-model="pattern"
            class="search-input"
            variant="underline"
            placeholder="Rechercher un mot-clé"
            @keydown.enter="onSearch"
            @focus="$emit('focus')"
            @blur="$emit('blur')"
            @click.stop
        />
    </div>
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";
import { SearchMixin } from "~/mixins";
import { folderUtils } from "@bluemind/mail";
import { mapState } from "vuex";

const { DEFAULT_FOLDERS } = folderUtils;

export default {
    name: "SearchInputMobile",
    components: { BmFormInput },
    mixins: [SearchMixin],
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        currentFolder() {
            return this.folders[this.activeFolder];
        }
    },
    mounted() {
        this.$refs.search.focus();
    },
    methods: {
        onSearch() {
            const folder = this.currentFolder.imapName === DEFAULT_FOLDERS.INBOX ? { key: null } : this.currentFolder;
            this.search(this.pattern, folder, true);
            this.$refs.search.blur();
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.search-input-mobile {
    & > .search-input {
        background: $surface;
        & > input {
            border-color: $neutral-fg-lo3;
        }
    }
}
</style>
