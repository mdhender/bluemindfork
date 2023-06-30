<template>
    <div class="bold pl-5 flex-fill text-truncate">
        {{ title }}
    </div>
</template>

<script>
export default {
    name: "BmNavbarTitle",
    props: {
        title: {
            type: String,
            required: true
        }
    }
};
</script>
