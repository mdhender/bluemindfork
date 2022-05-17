<template>
    <div class="too-large-box d-flex justify-content-around align-items-center">
        <img :src="tooLargellustration" alt="" class="ml-2 mr-1" />

        <div class="d-flex align-items-start">
            <div class="ml-4">
                <bm-icon icon="exclamation-circle-fill" class="text-danger"> </bm-icon>
            </div>
            <div class="ml-2">
                <i18n v-if="attachmentsCount > 1" path="mail.filehosting.threshold.hit" class="text-danger">
                    <template v-slot:hit>
                        {{ $tc("mail.filehosting.threshold.some_hit", attachmentsCount) }}
                    </template>
                    <template v-slot:size>
                        <strong class="font-weight-bold">{{ displaySize(sizeLimit) }}</strong>
                    </template>
                </i18n>

                <i18n v-else path="mail.filehosting.threshold.hit" class="text-danger">
                    <template v-slot:hit>
                        {{ $tc("mail.filehosting.threshold.size", attachmentsCount) }}
                    </template>
                    <template v-slot:size>
                        <strong class="font-weight-bold">{{ displaySize(sizeLimit) }}</strong>
                    </template>
                </i18n>

                <br />
                <span>{{ $tc("mail.filehosting.change_selection", attachmentsCount) }}</span>
            </div>
        </div>
    </div>
</template>

<script>
import { computeUnit } from "@bluemind/file-utils";
import { BmIcon } from "@bluemind/styleguide";
import tooLargellustration from "../../../../assets/too-large.png";

export default {
    name: "TooLargeBox",
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
        return {
            tooLargellustration
        };
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        }
    }
};
</script>
