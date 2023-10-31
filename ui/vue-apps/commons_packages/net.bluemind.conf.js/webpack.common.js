const VueLoaderPlugin = require("vue-loader/lib/plugin");

module.exports = {
    plugins: [new VueLoaderPlugin()],
    module: {
        rules: [
            {
                test: /\.vue$/,
                loader: "vue-loader"
            },
            {
                test: /\.css$/,
                use: ["vue-style-loader", "css-loader"]
            },
            {
                test: /\.scss$/,
                use: ["vue-style-loader", "css-loader", "sass-loader"]
            },
            {
                test: /\.svg$/,
                loader: "svg-inline-loader"
            },
            {
                test: /\.png$/,
                loader: "url-loader"
            },
            {
                test: /\.js$/,
                loader: "babel-loader",
                exclude: /node_modules/
            },
            {
                test: /\.ts$/,
                use: ["babel-loader", "ts-loader"],
                exclude: [/node_modules/, /target/]
            },

            {
                enforce: "pre",
                test: /\.(js|vue)$/,
                loader: "eslint-loader",
                exclude: /node_modules/
            }
        ]
    },
    externals: {
        vue: "Vue",
        "vue-router": "VueRouter",
        vuex: "Vuex"
    },
    resolve: {
        extensions: ["*", ".js", ".vue", ".json", ".css", ".scss", ".ts"]
    },
    devtool: "source-map"
};
