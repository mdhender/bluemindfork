export default {
    props: {
        localUserSettings: {
            type: Object,
            required: true
        },
        setting: {
            type: String,
            required: true
        },
        options: {
            type: Object,
            required: true
        },
        disabled: {
            type: Boolean,
            required: false,
            default: false
        }
    }
};
