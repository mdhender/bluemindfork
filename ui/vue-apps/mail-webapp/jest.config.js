const { TextEncoder, TextDecoder } = require("util");
const baseConfig = require("@bluemind/conf/jest.config.base");

module.exports = {
    name: "mail-webapp",
    displayName: "Mail WebApp",
    ...baseConfig,
    globals: {
        TextDecoder,
        TextEncoder
    },
    moduleNameMapper: {
        "^~/actions$": "<rootDir>/src/store/types/actions",
        "^~/getters$": "<rootDir>/src/store/types/getters",
        "^~/mutations$": "<rootDir>/src/store/types/mutations",
        "^!css-loader!sass-loader!(.*)$": "$1.scss",
        "~(.*)$": "<rootDir>/src$1"
    }
};
