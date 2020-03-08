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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hive.hcatalog.templeton.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Execute a local program.  This is a singleton service that will
 * execute a programs on the local box.
 * 
 * Note that is is executed from LaunchMapper which is executed in 
 * different JVM from WebHCat (Templeton) server.  Thus it should not call any classes
 * not available on every node in the cluster (outside webhcat jar)
 */
final class TrivialExecService {
  private static final Log LOG = LogFactory.getLog(TrivialExecService.class);
  private static volatile TrivialExecService theSingleton;
  /**
   * Retrieve the singleton.
   */
  public static synchronized TrivialExecService getInstance() {
    if (theSingleton == null)
      theSingleton = new TrivialExecService();
    return theSingleton;
  }
  public Process run(List<String> cmd, List<String> removeEnv,
                     Map<String, String> environmentVariables) throws IOException {
    LOG.info("run(cmd, removeEnv, environmentVariables)");
    LOG.info("Starting cmd: " + cmd);
    ProcessBuilder pb = new ProcessBuilder(cmd);
    for (String key : removeEnv) {
      if(pb.environment().containsKey(key)) {
        LOG.info("Removing env var: " + key + "=" + pb.environment().get(key));
      }
      pb.environment().remove(key);
    }
    pb.environment().putAll(environmentVariables);
    logDebugInfo("========Starting process with env:========", pb.environment());
    printContentsOfDir(".");
    return pb.start();
  }
  private static void logDebugInfo(String msg, Map<String, String> props) {
    StringBuilder sb = TempletonUtils.dumpPropMap(msg, props);
    LOG.info(sb.toString());
    String sqoopHome = props.get("SQOOP_HOME");
    if(TempletonUtils.isset(sqoopHome)) {
      //this is helpful when Sqoop is installed on each node in the cluster to make sure
      //relevant jars (JDBC in particular) are present on the node running the command
      printContentsOfDir(sqoopHome + File.separator+ "lib");
    }
  }
  /**
   * Print files and directories in current directory. Will list files in the sub-directory (only 1 level deep)
   * time honored tradition in WebHCat of borrowing from Oozie
   */
  private static void printContentsOfDir(String dir) {
    File folder = new File(dir);
    StringBuilder sb = new StringBuilder("Files in '").append(dir).append("' dir:").append(folder.getAbsolutePath()).append('\n');

    File[] listOfFiles = folder.listFiles();
    for (File fileName : listOfFiles) {
      if (fileName.isFile()) {
        sb.append("File: ").append(fileName.getName()).append('\n');
      }
      else if (fileName.isDirectory()) {
        sb.append("Dir: ").append(fileName.getName()).append('\n');
        File subDir = new File(fileName.getName());
        File[] moreFiles = subDir.listFiles();
        for (File subFileName : moreFiles) {
          if (subFileName.isFile()) {
            sb.append("--File: ").append(subFileName.getName()).append('\n');
          }
          else if (subFileName.isDirectory()) {
            sb.append("--Dir: ").append(subFileName.getName()).append('\n');
          }
        }
      }
    }
    LOG.info(sb.toString());
  }
}
