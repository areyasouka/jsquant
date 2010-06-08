dependencies = {
    layers: [
        {
        name: "jsquant-all.js",
        dependencies: [
                       "dojo.DeferredList",
                       "dojo.number",
                       "dojo.date.locale",
                       "dojo.io.script",
                       "dijit.TooltipDialog",
                       "dijit.layout.BorderContainer",
                       "dijit.layout.TabContainer",
                       "dijit.layout.ContentPane",
                       "dijit.form.DropDownButton",
                       "dijit.Menu",
                       "dojox.widget.PlaceholderMenuItem",
                       "dojox.analytics.Urchin",
                       "dojox.data.CsvStore",
                       "dojox.grid.DataGrid"
        ]
        }
    ],
    prefixes: [
        [ "dijit", "../dijit" ],
        [ "dojox", "../dojox" ],
        [ "jsquant", "../jsquant" ]
    ]
};
