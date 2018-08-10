/*
 * Copyright 2013 Monocrea Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sitoolkit.wt.domain.tester;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.sitoolkit.wt.domain.debug.DebugSupport;
import org.sitoolkit.wt.domain.evidence.DialogScreenshotSupport;
import org.sitoolkit.wt.domain.evidence.Evidence;
import org.sitoolkit.wt.domain.evidence.EvidenceManager;
import org.sitoolkit.wt.domain.evidence.LogLevelVo;
import org.sitoolkit.wt.domain.evidence.LogRecord;
import org.sitoolkit.wt.domain.evidence.Screenshot;
import org.sitoolkit.wt.domain.evidence.ScreenshotTaker;
import org.sitoolkit.wt.domain.evidence.ScreenshotTiming;
import org.sitoolkit.wt.domain.operation.OperationResult;
import org.sitoolkit.wt.domain.testscript.TestScript;
import org.sitoolkit.wt.domain.testscript.TestScriptCatalog;
import org.sitoolkit.wt.domain.testscript.TestScriptDao;
import org.sitoolkit.wt.domain.testscript.TestStep;
import org.sitoolkit.wt.infra.PropertyManager;
import org.sitoolkit.wt.infra.TestException;
import org.sitoolkit.wt.infra.VerifyException;
import org.sitoolkit.wt.infra.log.SitLogger;
import org.sitoolkit.wt.infra.log.SitLoggerFactory;
import org.sitoolkit.wt.infra.resource.MessageManager;

/**
 * このクラスは、テストの実施者を表すエンティティです。
 *
 * @author yuichi.kuwahara
 */
public class Tester {

    protected final SitLogger log = SitLoggerFactory.getLogger(getClass());

    @Resource
    TestContext current;
    @Resource
    DebugSupport debug;

    @Resource
    DialogScreenshotSupport dialog;

    @Resource
    EvidenceManager em;

    @Resource
    ScreenshotTaker screenshotTaker;

    @Resource
    TestScriptDao dao;

    @Resource
    TestScriptCatalog catalog;

    @Resource
    PropertyManager pm;

    @Resource
    OperationSupport operationSupport;

    // /**
    // * スクリーンショット操作
    // */
    // private ScreenshotOperation screenshotOpe;
    /**
     * テストスクリプトがロード済の場合にtrue
     */
    private boolean scriptLoaded = false;
    private TestScript testScript;

    public void setUp(String caseNo) {
        current.reset();
        current.setCaseNo(caseNo);
        current.setScriptName(testScript.getName());

        // opelog.beginScript();
    }

    public void prepare(String scriptPath, String sheetName, String caseNo) {
        testScript = catalog.get(scriptPath, sheetName);
        current.setTestScript(testScript);
        current.setScriptName(testScript.getName());
        current.reset();
        log.debug("prepare.test", current);
    }

    /**
     * テストスクリプトファイルを読み込み内部に保持します。
     *
     * @param testScriptPath
     *            テストスクリプトのパス
     */
    public void setUpClass(String testScriptPath, String sheetName) {
        if (!isScriptLoaded()) {
            log.info("script.load2", testScriptPath, sheetName);
            testScript = catalog.get(testScriptPath, sheetName);
            current.setTestScript(testScript);

            if (pm.isDebug()) {
                log.info("test.step.last");
                TestStep lastStep = testScript.getLastStep();
                lastStep.setBreakPoint("y");
            }

        }
    }

    public void tearDown() {
        // NOP
    }

    /**
     * ケース番号のテストスクリプトを実行します。
     *
     * @param caseNo
     *            ケース番号
     * @return テスト結果
     */
    public TestResult operate(String caseNo) {

        if (StringUtils.isEmpty(caseNo)) {
            caseNo = testScript.getCaseNoMap().keySet().iterator().next();
        } else if (!testScript.containsCaseNo(caseNo)) {
            String msg = MessageManager.getMessage("case.number.error", caseNo)
                    + testScript.getCaseNoMap().keySet();
            throw new TestException(msg);
        }

        current.setCaseNo(caseNo);

        dialog.checkReserve(testScript.getTestStepList(), caseNo);
        log.info("case.execute", caseNo);

        List<Exception> ngList = new ArrayList<Exception>();
        TestResult result = new TestResult();

        Evidence evidence = em.createEvidence(current.getScriptName(), caseNo);
        TestStep testStep = null;

        if (debug.isInterrupted()) {
            result.setInterrupted(debug.isInterrupted());
            return result;
        }
        try {
            debug.start();
            do {
                testStep = current.getTestStep();
                dialog.reserveWindowRect(testStep.getNo());

                try {

                    operateOneScript(testStep, current.getCaseNo(), evidence);

                } catch (VerifyException e) {
                    ngList.add(e);
                    result.add(e);
                    evidence.addLogRecord(LogRecord.create(log, LogLevelVo.ERROR, testStep,
                            "unexpected.result", e.getLocalizedMessage()));
                    if (!operationSupport.isDbVerify(testStep.getOperation())) {
                        addScreenshot(evidence, ScreenshotTiming.ON_ERROR);
                    }

                    if (pm.isDebug()) {
                        debug.pause();
                    }

                } catch (Exception e) {

                    if (pm.isDebug()) {
                        ngList.add(e);
                        evidence.addLogRecord(LogRecord.create(log, LogLevelVo.ERROR, testStep,
                                "unexpected.error2", e.getLocalizedMessage()));
                        log.debug("exception", e);
                        if (!operationSupport.isDbVerify(testStep.getOperation())) {
                            addScreenshot(evidence, ScreenshotTiming.ON_ERROR);
                        }
                        debug.pause();
                    } else {
                        throw e;
                    }

                }

            } while (debug.next());

            result.setInterrupted(debug.isInterrupted());

        } catch (Exception e) {
            evidence.addLogRecord(LogRecord.create(log, LogLevelVo.ERROR, testStep,
                    "unexpected.error2", e.getLocalizedMessage()));
            log.debug("exception", e);
            if (!operationSupport.isDbVerify(testStep.getOperation())) {
                addScreenshot(evidence, ScreenshotTiming.ON_ERROR);
            }
            result.setErrorCause(e);
        } finally {
            em.flushEvidence(evidence);
        }

        return result;
    }

    /**
     * 1件のテストステップを実施します。 テストステップに指示がある場合、 実施前または後でスクリーンショットを取得します。
     *
     * @param testStep
     *            テストステップ
     * @param caseNo
     *            ケース番号
     * @see TestStep#execute(int)
     * @see TestStep#beforeScreenshot()
     * @see TestStep#afterScreenshot()
     */
    private void operateOneScript(TestStep testStep, String caseNo, Evidence evidence) {
        testStep.setCurrentCaseNo(caseNo);

        if (testStep.isSkip()) {
            log.info("case.skip", caseNo, testStep.getNo(), testStep.getItemName());
            return;
        }

        if (testStep.dialogScreenshot()) {
            addScreenshot(evidence, ScreenshotTiming.ON_DIALOG);
        } else if (testStep.beforeScreenshot()) {
            addScreenshot(evidence, ScreenshotTiming.BEFORE_OPERATION);
        }

        OperationResult result = testStep.getOperation().operate(testStep);
        evidence.addLogRecords(result.getRecords());

        if (testStep.afterScreenshot()) {
            addScreenshot(evidence, ScreenshotTiming.AFTER_OPERATION);
        }

        evidence.commitScreenshot();

        try {
            Thread.sleep(pm.getOperationWait());
        } catch (InterruptedException e) {
            log.warn("thread.sleep.error", e);
        }
    }

    private void addScreenshot(Evidence evidence, ScreenshotTiming timing) {
        Screenshot screenshot = screenshotTaker.get(timing);
        evidence.addScreenshot(screenshot, current.getScreenshotTiming());
        em.moveScreenshot(evidence, current.getTestStepNo(), current.getItemName());
    }

    public boolean isScriptLoaded() {
        return scriptLoaded;
    }

    public TestScript getTestScript() {
        return testScript;
    }

}
