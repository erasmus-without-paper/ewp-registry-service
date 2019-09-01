import Vue from 'vue';
import apiEntry from "./apiEntry";
import helpPopup from "./helpPopup";

/*
new Vue({
    el: '#api-entry-set',
    components: {
        "apiEntry": apiEntry,
        "helpPopup": helpPopup
    },
    template: '<api-entry/>',
});
 */

$(function() {
    new Vue({
        el: '#api-entry-set',
        components: {
            "apiEntry": apiEntry,
            "helpPopup": helpPopup
        }
    });
});
