<template>
    <bm-chip
        variant="caption"
        class="bm-more-items-badge position-absolute"
        :class="{ 'd-none': !count }"
        v-on="$listeners"
    >
        <span>+ {{ count }}</span>
    </bm-chip>
</template>

<script>
import BmChip from "./BmChip";

export default {
    name: "BmMoreItemsBadge",
    components: { BmChip },
    props: {
        count: {
            type: Number,
            required: true
        },
        active: {
            type: Boolean,
            default: true
        }
    },
    methods: {
        /** /!\ Works with OverflownElements directive. */
        hideOverflownElements({ overflownEvent, elementClass }) {
            // hide overflown elements
            let hiddenElementCount = 0;
            let firstHiddenElementLeftPos;
            let lastVisibleElement;
            for (const { element, overflows } of overflownEvent.detail) {
                if (!elementClass || element.classList.contains(elementClass)) {
                    if (overflows && this.active) {
                        if (hiddenElementCount === 0) {
                            firstHiddenElementLeftPos = element.offsetLeft;
                        }
                        element.style.visibility = "hidden";
                        hiddenElementCount++;
                    } else {
                        element.style.visibility = "visible";
                        lastVisibleElement = element;
                    }
                }
            }

            // move the badge to the left, toward the last visible element
            if (hiddenElementCount > 0) {
                const badgeElement = this.$el;
                const parentElement = badgeElement.parentElement;
                badgeElement.style.left = `${firstHiddenElementLeftPos}px`;
                badgeElement.style.height = `${parentElement.offsetHeight}px`;
                badgeElement.classList.replace("d-none", "d-flex");
                // adjust visible elements and badge position
                if (
                    firstHiddenElementLeftPos + badgeElement.offsetWidth >
                    parentElement.offsetLeft + parentElement.offsetWidth
                ) {
                    lastVisibleElement.style.visibility = "hidden";
                    hiddenElementCount++;
                    firstHiddenElementLeftPos = lastVisibleElement.offsetLeft;
                    badgeElement.style.left = `${firstHiddenElementLeftPos}px`;
                }
            }

            return hiddenElementCount;
        }
    }
};
</script>

<style lang="scss">
@import "../css/_variables.scss";

.bm-more-items-badge {
    &.bm-chip:hover {
        cursor: pointer;
    }
}
</style>
