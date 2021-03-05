<template>
    <bm-form-radio-group v-model="localUserSettings[setting]" class="pref-field-choice d-flex flex-wrap">
        <bm-form-radio
            v-for="choice in options.choices"
            :key="choice.value"
            class="ml-5"
            :value="choice.value"
            :aria-label="choice.name"
        >
            <template #img>
                <img v-if="choice.img" :src="choice.img" alt="null" />
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div v-if="choice.svg" v-html="choice.svg" />
            </template>

            <template>
                {{ choice.name }}
            </template>
        </bm-form-radio>
    </bm-form-radio-group>
</template>

<script>
import { BmFormRadio, BmFormRadioGroup } from "@bluemind/styleguide";
import PrefFieldMixin from "../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldChoice",
    components: {
        BmFormRadio,
        BmFormRadioGroup
    },
    mixins: [PrefFieldMixin]
};
</script>
