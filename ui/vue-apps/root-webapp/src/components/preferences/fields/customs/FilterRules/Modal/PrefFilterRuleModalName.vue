<template>
    <bm-form-group
        id="name-group"
        class="pref-filter-rule-modal-name"
        :label="$t('preferences.mail.filters.modal.name')"
        label-for="pref-filter-rule-modal-name-input"
        label-class="circled-number one d-flex align-items-center"
        :invalid-feedback="$t('preferences.mail.filters.modal.name.empty')"
    >
        <bm-form-input
            id="pref-filter-rule-modal-name-input"
            ref="name-input"
            v-model="name"
            variant="underline"
            :placeholder="$t('preferences.mail.filters.modal.name.placeholder')"
            :state="inputState"
            required
            @keypress.enter.prevent="$emit('submit')"
        />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmFormInput } from "@bluemind/ui-components";
export default {
    name: "PrefFilterRuleModalName",
    components: { BmFormGroup, BmFormInput },
    props: {
        filter: {
            type: Object,
            required: true
        }
    },
    computed: {
        name: {
            get() {
                return this.filter.name;
            },
            set(name) {
                this.$emit("update:filter", { ...this.filter, name });
            }
        },
        inputState() {
            return this.name?.trim() ? null : false;
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "./variables";

.pref-filter-rule-modal-name {
    @include from-lg {
        #pref-filter-rule-modal-name-input {
            width: calc(100% - $close-gutter);
        }
    }
}
</style>
