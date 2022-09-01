<template>
    <bm-form-radio-group v-model="value" class="pref-field-choice d-flex flex-wrap py-5">
        <bm-form-radio v-for="choice in choices" :key="choice.value" :value="choice.value" :aria-label="choice.name">
            <template #img>
                <template v-if="!collapsed">
                    <img v-if="choice.img" :src="choice.img" alt="null" />
                    <!-- eslint-disable-next-line vue/no-v-html -->
                    <div v-if="choice.svg" v-html="choice.svg" />
                </template>
            </template>
            {{ choice.name }}
        </bm-form-radio>
    </bm-form-radio-group>
</template>

<script>
import { BmFormRadio, BmFormRadioGroup } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";

export default {
    name: "PrefFieldChoice",
    components: { BmFormRadio, BmFormRadioGroup },
    mixins: [OneSettingField],
    props: {
        choices: {
            type: Array,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.pref-field-choice {
    gap: $sp-5;
    @include from-lg {
        gap: $sp-8;
    }
}
</style>
