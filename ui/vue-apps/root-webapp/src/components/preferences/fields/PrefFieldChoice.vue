<template>
    <bm-form-radio-group
        v-model="value"
        class="pref-field-choice d-flex flex-wrap"
        :class="{ 'image-mode': imageMode, 'flex-column py-5': !imageMode }"
    >
        <bm-form-radio v-for="choice in choices" :key="choice.value" :value="choice.value" :aria-label="choice.name">
            <template v-if="imageMode" #img>
                <img v-if="choice.img" :src="choice.img" alt="null" />
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div v-if="choice.svg" v-html="choice.svg" />
            </template>
            <span class="text-neutral">{{ choice.name }}</span>
        </bm-form-radio>
    </bm-form-radio-group>
</template>

<script>
import { BmFormRadio, BmFormRadioGroup } from "@bluemind/ui-components";
import OneSettingField from "../mixins/OneSettingField";

export default {
    name: "PrefFieldChoice",
    components: { BmFormRadio, BmFormRadioGroup },
    mixins: [OneSettingField],
    props: { choices: { type: Array, required: true } },
    computed: {
        imageMode() {
            return this.choices.every(({ svg, img }) => Boolean(svg) || Boolean(img));
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-field-choice {
    gap: $sp-5;
    &.image-mode {
        @include from-lg {
            gap: $sp-8;
        }
    }
}
</style>
