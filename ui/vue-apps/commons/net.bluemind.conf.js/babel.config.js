module.exports = function(api, otherPlugins = []) {
    api.cache(true);

    const presets = [
        [
            "@babel/preset-env",
            {
                modules: "auto",
                targets: {
                    browsers: ["> 0.25%", "not dead", "not ie <= 8"],
                    node: 8
                }
            }
        ],
        "@babel/preset-typescript"
    ];

    const plugins = otherPlugins;

    if (process.env.NODE_ENV === "test") {
        plugins.push("require-context-hook");
    }

    return {
        presets,
        plugins
    };
};
