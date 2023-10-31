import { AlertTypes } from "@bluemind/alert.store";

export default {
    methods: {
        variant({ type }) {
            switch (type) {
                case AlertTypes.LOADING:
                    return "neutral";
                case AlertTypes.INFO:
                    return "info";
                case AlertTypes.SUCCESS:
                    return "success";
                case AlertTypes.WARNING:
                    return "warning";
                case AlertTypes.ERROR:
                    return "danger";
            }
        }
    }
};
