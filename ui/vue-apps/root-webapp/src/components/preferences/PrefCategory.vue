<template>
    <div :id="anchor(category)" class="pref-category">
        <template v-for="group in category.groups">
            <pref-group v-if="group.visible" :key="group.id" :group="group" />
        </template>
    </div>
</template>

<script>
import Navigation from "./mixins/Navigation";
import PrefGroup from "./PrefGroup";

export default {
    name: "PrefCategory",
    components: { PrefGroup },
    mixins: [Navigation],
    props: {
        category: {
            type: Object,
            required: true
        }
    }
};
</script>
