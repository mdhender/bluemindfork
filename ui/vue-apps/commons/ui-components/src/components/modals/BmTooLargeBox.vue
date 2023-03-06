<template>
    <div class="too-large-box">
        <bm-illustration value="oversize-file" size="sm" />
        <div class="spacer"></div>
        <div class="d-flex align-items-start">
            <bm-icon icon="exclamation-circle-fill" class="text-danger" size="lg" />
            <div class="ml-4">
                <i18n :path="i18nPath" class="text-danger">
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
import BmIllustration from "../BmIllustration";

export default {
    name: "BmTooLargeBox",
    components: { BmIcon, BmIllustration },
    props: {
        attachmentsCount: {
            type: Number,
            required: true
        },
        sizeLimit: {
            type: Number,
            required: true
        },
        i18nPath: {
            type: String,
            default: "mail.actions.attach.max_size"
        }
    },
    computed: {
        displayedSize() {
            return computeUnit(this.sizeLimit, this.$i18n);
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";

.too-large-box {
    display: flex;
    justify-content: space-between;
    align-items: center;

    & > .spacer {
        flex: 0 2 $sp-7;
        min-width: $sp-3;
    }

    .bm-illustration {
        flex: none;
        width: 120px;
        height: 130px;
        position: relative;

        & > svg {
            position: absolute;
            left: -50px;
            top: -28px;
        }
    }
}
</style>
