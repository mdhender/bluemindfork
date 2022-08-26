<template>
    <bm-icon v-if="isFhFile" class="cloud-icon" icon="cloud" size="xs" />
</template>

<script>
import { mapGetters } from "vuex";
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "CloudIcon",
    components: { BmIcon },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_FILE"]),
        isFhFile() {
            return this.GET_FH_FILE(this.file);
        }
    }
};
</script>
