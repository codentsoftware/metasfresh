--
-- insert MI to enable remote cache invalidation
--
-- 2018-01-04T16:04:06.301
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
INSERT INTO AD_ModelValidator (AD_Client_ID,AD_ModelValidator_ID,AD_Org_ID,Created,CreatedBy,Description,EntityType,IsActive,ModelValidationClass,Name,SeqNo,Updated,UpdatedBy) VALUES (0,540119,0,TO_TIMESTAMP('2018-01-04 16:04:06','YYYY-MM-DD HH24:MI:SS'),100,'','de.metas.material.cockpit','Y','de.metas.material.cockpit.interceptor.ModuleInterceptor','material-cockpit',0,TO_TIMESTAMP('2018-01-04 16:04:06','YYYY-MM-DD HH24:MI:SS'),100)
;

--
-- make MD_EventLog.EventTime a range filter
--
-- 2018-01-04T16:05:39.442
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
UPDATE AD_Column SET IsRangeFilter='Y', IsSelectionColumn='Y',Updated=TO_TIMESTAMP('2018-01-04 16:05:39','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Column_ID=558411
;

