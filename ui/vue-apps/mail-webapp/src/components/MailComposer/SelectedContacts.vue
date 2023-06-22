<template>
    <div class="selected-contacts selected d-flex bg-surface">
        <contact-input
            :contacts.sync="selectedContacts"
            readonly
            variant="inline"
            class="mx-6 d-flex flex-fill align-items-center"
        >
            <span class="selected-label">{{ $t("common.to") }}</span>
        </contact-input>
    </div>
</template>

<script>
import { ContactInput } from "@bluemind/business-components";

export default {
    name: "SelectedContacts",
    components: { ContactInput },
    props: {
        contacts: { type: Array, default: undefined }
    },
    computed: {
        selectedContacts: {
            get() {
                return this.contacts;
            },
            set(value) {
                this.$emit("update:contacts", value);
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";
@import "@bluemind/ui-components/src/css/type";
@import "@bluemind/ui-components/src/css/variables";

.selected-contacts {
    min-height: $avatar-height-sm + base-px-to-rem(20);
    .contact-input {
        .selected-label {
            color: $neutral-fg-hi1;
            @extend %bold;
            margin-right: $sp-5;
            @include from-lg {
                margin-right: $sp-6;
            }
            height: 100%;
            vertical-align: top;
            padding-top: base-px-to-rem(12);
        }
    }
    .contact-and-input:first-of-type .contact {
        margin-left: 0;
    }
}
</style>
