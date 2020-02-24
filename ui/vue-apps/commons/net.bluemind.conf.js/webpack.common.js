var path = require("path");
const VueLoaderPlugin = require("vue-loader/lib/plugin");

module.exports = {
    plugins: [new VueLoaderPlugin()],
    module: {
        rules: [
            {
                enforce: "pre",
                test: /\.(js|vue)$/,
                loader: "eslint-loader",
                exclude: /node_modules/
            },
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
                include: [path.resolve(__dirname, "./src")]
            }
        ]
    },
    externals: {
        vue: "Vue",
        "vue-router": "VueRouter",
        vuex: "Vuex"
    },
    resolve: {
        extensions: ["*", ".js", ".vue", ".json", ".css", ".scss"]
    }
};
