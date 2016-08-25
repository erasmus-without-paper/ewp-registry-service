alter table WATCHER_STATUS rename to RECIPIENTS;
alter table RECIPIENTS alter column last_status_level_reported rename to currently_reported_flag_state;
