<template>
    <bm-icon v-if="isFhAttachment" class="cloud-icon mr-1" icon="cloud" />
</template>

<script>
import { mapGetters } from "vuex";
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "CloudIcon",
    components: { BmIcon },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_ATTACHMENT"]),
        isFhAttachment() {
            return this.GET_FH_ATTACHMENT(this.message, this.attachment);
        }
    }
};
</script>
