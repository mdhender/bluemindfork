<template>
    <div class="mail-composer-sender d-flex flex-column justify-content-between">
        <div class="d-flex align-items-center flex-fill">
            <div class="label" :class="labelClass">{{ $t("common.from") }}</div>
            <bm-form-select
                ref="identity-chooser"
                :value="{ email: message.from.address, displayname: message.from.dn }"
                :options="options"
                :auto-min-width="false"
                class="flex-fill"
                variant="inline"
                @input="changeIdentity"
            />
        </div>
        <hr class="m-0" />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmFormSelect } from "@bluemind/ui-components";

export default {
    name: "MailComposerSender",
    components: { BmFormSelect },
    props: {
        message: {
            type: Object,
            required: true
        },
        labelClass: {
            type: String,
            default: ""
        }
    },
    computed: {
        ...mapState("root-app", ["identities"]),
        options() {
            return this.identities.map(i => ({
                text: i.displayname ? `${i.displayname} <${i.email}>` : i.email,
                value: { email: i.email, displayname: i.displayname }
            }));
        }
    },
    async mounted() {
        this.$refs["identity-chooser"].focus();
    },
    methods: {
        changeIdentity(identity) {
            if (identity !== undefined) {
                this.$emit("update", identity);
            } else {
                this.$emit("check-and-repair");
            }
        }
    }
};
</script>

<style lang="scss">
.mail-composer-sender {
    .label {
        flex: none;
    }
}
</style>
