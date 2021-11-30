<template>
    <bm-form-radio-group v-model="value" class="pref-field-choice d-flex flex-wrap">
        <bm-form-radio
            v-for="choice in choices"
            :key="choice.value"
            class="ml-5"
            :value="choice.value"
            :aria-label="choice.name"
        >
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
