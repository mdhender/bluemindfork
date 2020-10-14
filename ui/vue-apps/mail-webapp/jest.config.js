const baseConfig = require("@bluemind/conf/jest.config.base");

module.exports = {
    name: "mail-webapp",
    displayName: "Mail WebApp",
    ...baseConfig,
    moduleNameMapper: {
        "^~/actions$": "<rootDir>/src/store/types/actions",
        "^~/getters$": "<rootDir>/src/store/types/getters",
        "^~/mutations$": "<rootDir>/src/store/types/mutations",
        "~(.*)$": "<rootDir>/src$1"
    }
};
