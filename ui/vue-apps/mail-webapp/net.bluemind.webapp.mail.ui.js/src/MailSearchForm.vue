<template>
    <bm-form-input
        :value="pattern"
        :placeholder="$t('common.search')"
        type="search"
        icon="search"
        :aria-label="$t('common.search')"
        class="mail-search-form rounded-0"
        @input="onChange"
        @keydown.enter="updateRoute"
        @reset="cancel"
    />
</template>

<script>
import { BmFormInput } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";
import debounce from "lodash.debounce";

const SPINNER_TIMEOUT = 50;
const UPDATE_ROUTE_TIMEOUT = 500;

export default {
    name: "MailSearchForm",
    components: { BmFormInput },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapState("mail-webapp/search", ["pattern"])
    },
    methods: {
        ...mapMutations("mail-webapp/search", ["setStatus"]),
        updateRoute: debounce(function(value) {
            if (value) {
                this.$router.push("/mail/search/" + value + "/");
            } else {
                this.cancel();
            }
        }, UPDATE_ROUTE_TIMEOUT),
        cancel() {
            this.$router.push("/mail/" + this.currentFolderKey + "/");
        },
        showSpinner: debounce(function() {
            this.setStatus("loading");
        }, SPINNER_TIMEOUT),
        onChange(value) {
            this.showSpinner();
            this.updateRoute(value);
        }
    }
};
</script>
