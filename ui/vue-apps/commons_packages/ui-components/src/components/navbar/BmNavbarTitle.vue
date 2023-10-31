<template>
    <div class="bm-navbar-title bold pl-5 flex-fill text-truncate">
        <slot>
            {{ title }}
        </slot>
    </div>
</template>

<script>
export default {
    name: "BmNavbarTitle",
    props: {
        title: {
            type: String,
            default: ""
        }
    }
};
</script>
