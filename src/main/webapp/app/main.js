import { createApp } from 'vue';
import apiEntry from "./apiEntry";
import helpPopup from "./helpPopup";

createApp({})
    .component('apiEntry', apiEntry)
    .component('helpPopup', helpPopup)
    .mount('#api-entry-set');
