module.exports = function (api, otherPlugins = []) {
    api.cache(true);
  
    const presets = [
        ["@babel/preset-env", 
            {
                "modules": "auto",
                "targets": {
                    "browsers": [
                        "> 0.25%",
                        "not dead",
                        "not ie <= 8"
                    ],
                    "node": 8
                }
            }
        ]
    ];

    const plugins = otherPlugins;
  
    return {
        presets,
        plugins
    };
};