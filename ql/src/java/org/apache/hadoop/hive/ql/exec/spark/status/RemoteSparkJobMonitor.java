/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec.spark.status;

import java.util.Map;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.exec.spark.status.impl.RemoteSparkJobStatus;
import org.apache.hadoop.hive.ql.log.PerfLogger;
import org.apache.hive.spark.client.JobHandle;
import org.apache.spark.JobExecutionStatus;

/**
 * RemoteSparkJobMonitor monitor a RSC remote job status in a loop until job finished/failed/killed.
 * It print current job status to console and sleep current thread between monitor interval.
 */
public class RemoteSparkJobMonitor extends SparkJobMonitor {

  private RemoteSparkJobStatus sparkJobStatus;
  private final HiveConf hiveConf;

  public RemoteSparkJobMonitor(HiveConf hiveConf, RemoteSparkJobStatus sparkJobStatus) {
    super(hiveConf);
    this.sparkJobStatus = sparkJobStatus;
    this.hiveConf = hiveConf;
  }

  @Override
  public int startMonitor() {
    boolean running = false;
    boolean done = false;
    int rc = 0;
    Map<String, SparkStageProgress> lastProgressMap = null;

    perfLogger.PerfLogBegin(CLASS_NAME, PerfLogger.SPARK_RUN_JOB);
    perfLogger.PerfLogBegin(CLASS_NAME, PerfLogger.SPARK_SUBMIT_TO_RUNNING);

    long startTime = System.currentTimeMillis();

    while (true) {
      try {
        JobHandle.State state = sparkJobStatus.getRemoteJobState();
        if (LOG.isDebugEnabled()) {
          console.printInfo("state = " + state);
        }

        switch (state) {
        case SENT:
        case QUEUED:
          long timeCount = (System.currentTimeMillis() - startTime) / 1000;
          if ((timeCount > monitorTimeoutInteval)) {
            console.printError("Job hasn't been submitted after " + timeCount + "s." +
                " Aborting it.\nPossible reasons include network issues, " +
                "errors in remote driver or the cluster has no available resources, etc.\n" +
                "Please check YARN or Spark driver's logs for further information.");
            console.printError("Status: " + state);
            running = false;
            done = true;
            rc = 2;
          }
          break;
        case STARTED:
          JobExecutionStatus sparkJobState = sparkJobStatus.getState();
          if (sparkJobState == JobExecutionStatus.RUNNING) {
            Map<String, SparkStageProgress> progressMap = sparkJobStatus.getSparkStageProgress();
            if (!running) {
              perfLogger.PerfLogEnd(CLASS_NAME, PerfLogger.SPARK_SUBMIT_TO_RUNNING);
              printAppInfo();
              // print job stages.
              console.printInfo("\nQuery Hive on Spark job["
                + sparkJobStatus.getJobId() + "] stages:");
              for (int stageId : sparkJobStatus.getStageIds()) {
                console.printInfo(Integer.toString(stageId));
              }

              console.printInfo("\nStatus: Running (Hive on Spark job["
                + sparkJobStatus.getJobId() + "])");
              running = true;

              console.printInfo("Job Progress Format\nCurrentTime StageId_StageAttemptId: "
                + "SucceededTasksCount(+RunningTasksCount-FailedTasksCount)/TotalTasksCount [StageCost]");
            }

            printStatus(progressMap, lastProgressMap);
            lastProgressMap = progressMap;
          }
          break;
        case SUCCEEDED:
          Map<String, SparkStageProgress> progressMap = sparkJobStatus.getSparkStageProgress();
          printStatus(progressMap, lastProgressMap);
          lastProgressMap = progressMap;
          double duration = (System.currentTimeMillis() - startTime) / 1000.0;
          console.printInfo("Status: Finished successfully in "
            + String.format("%.2f seconds", duration));
          running = false;
          done = true;
          break;
        case FAILED:
          console.printError("Status: Failed");
          running = false;
          done = true;
          rc = 3;
          break;
        case CANCELLED:
          console.printInfo("Status: Cancelled");
          running = false;
          done = true;
          rc = 3;
          break;
        }

        if (!done) {
          Thread.sleep(checkInterval);
        }
      } catch (Exception e) {
        String msg = " with exception '" + Utilities.getNameMessage(e) + "'";
        msg = "Failed to monitor Job[ " + sparkJobStatus.getJobId() + "]" + msg;

        // Has to use full name to make sure it does not conflict with
        // org.apache.commons.lang.StringUtils
        LOG.error(msg, e);
        console.printError(msg, "\n" + org.apache.hadoop.util.StringUtils.stringifyException(e));
        rc = 1;
        done = true;
      } finally {
        if (done) {
          break;
        }
      }
    }

    perfLogger.PerfLogEnd(CLASS_NAME, PerfLogger.SPARK_RUN_JOB);
    return rc;
  }

  private void printAppInfo() {
    String sparkMaster = hiveConf.get("spark.master");
    if (sparkMaster != null && sparkMaster.startsWith("yarn")) {
      String appID = sparkJobStatus.getAppID();
      if (appID != null) {
        console.printInfo("Running with YARN Application = " + appID);
        console.printInfo("Kill Command = " +
            HiveConf.getVar(hiveConf, HiveConf.ConfVars.YARNBIN) + " application -kill " + appID);
      }
    }
  }
}
