/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hive.ptest.execution.ssh;

import org.apache.hive.ptest.execution.Constants;

public class SSHResult extends AbstractSSHResult {

  private final String command;
  public SSHResult(String user, String host, int instance, String command,
      int exitCode, Exception exception, String output) {
    super(user, host, instance, exitCode, exception, output);
    this.command = command;
  }
  public String getCommand() {
    return command;
  }
  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("SSHResult [command=");
    ret.append(command)
        .append(", getExitCode()=")
        .append(getExitCode())
        .append(", getException()=")
        .append(getException())
        .append(", getUser()=")
        .append(getUser())
        .append(", getHost()=")
        .append(getHost())
        .append(", getInstance()=")
        .append(getInstance());
    if(getExitCode() == Constants.EXIT_CODE_UNKNOWN
        || getExitCode() == Constants.EXIT_CODE_EXCEPTION) {
      ret.append(", getOutput()=")
          .append(getOutput());
    }
    ret.append(" ]");
    return ret.toString();
  }
}
