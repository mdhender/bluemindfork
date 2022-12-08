<template>
    <div class="too-large-box d-flex justify-content-around align-items-center">
        <img :src="tooLargellustration" alt="" class="ml-2 mr-1" />

        <div class="d-flex align-items-start">
            <bm-icon icon="exclamation-circle-fill" class="text-danger ml-4" size="lg" />
            <div class="ml-2">
                <i18n path="common.threshold.hit" class="text-danger">
                    <template v-slot:hit>
                        <slot />
                    </template>
                    <template v-slot:size>
                        <strong class="font-weight-bold">{{ displayedSize }}</strong>
                    </template>
                </i18n>

                <br />
                <span>{{ $tc("styleguide.modal.too_large_box.threshold.change_selection", attachmentsCount) }}</span>
            </div>
        </div>
    </div>
</template>

<script>
import { computeUnit } from "@bluemind/file-utils";
import BmIcon from "../BmIcon";
import tooLargellustration from "./too-large.png";

export default {
    name: "BmTooLargeBox",
    components: { BmIcon },
    props: {
        attachmentsCount: {
            type: Number,
            required: true
        },
        sizeLimit: {
            type: Number,
            required: true
        }
    },
    data: function () {
        return { tooLargellustration };
    },
    computed: {
        displayedSize() {
            return computeUnit(this.sizeLimit, this.$i18n);
        }
    }
};
</script>
