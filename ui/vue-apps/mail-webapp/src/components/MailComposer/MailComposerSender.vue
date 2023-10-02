<template>
    <div class="mail-composer-sender d-flex flex-column justify-content-between">
        <div class="d-flex align-items-center flex-fill">
            <div class="label" :class="labelClass">{{ $t("common.from") }}</div>
            <bm-form-select
                ref="identity-chooser"
                :value="message.from.id || 'default'"
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
                text: this.identityToSenderText(i),
                value: i.id
            }));
        }
    },
    async mounted() {
        this.$refs["identity-chooser"].focus();
    },
    methods: {
        changeIdentity(identityId) {
            const identity = this.identities.find(({ id }) => id === identityId);
            identity !== undefined ? this.$emit("update", identity) : this.$emit("check-and-repair");
        },
        identityToSenderText(identity) {
            let text = identity.displayname ? `${identity.displayname} <${identity.email}>` : identity.email;
            if (identity.name && identity.name !== identity.displayname) {
                text += ` (${identity.name})`;
            }
            return text;
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
