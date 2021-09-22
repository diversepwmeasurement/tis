/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.qlangtech.tis.datax;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TISCollectionUtils;
import com.qlangtech.tis.order.center.IParamContext;
import org.apache.commons.exec.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 独立进程中执行DataX任务，这样可以有效避免每次执行DataX任务由于ClassLoader的冲突导致的错误
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-29 10:19
 **/
public abstract class DataXJobSingleProcessorExecutor implements QueueConsumer<CuratorDataXTaskMessage> {
    private static final Logger logger = LoggerFactory.getLogger(DataXJobSingleProcessorExecutor.class);

    // 记录当前正在执行的任务<taskid,ExecuteWatchdog>
    public final ConcurrentHashMap<Integer, ExecuteWatchdog> runningTask = new ConcurrentHashMap<>();

    @Override
    public void consumeMessage(CuratorDataXTaskMessage msg) throws Exception {
        //MDC.put();
        Integer jobId = msg.getJobId();
        String jobName = msg.getJobName();
        String dataxName = msg.getDataXName();
        MDC.put(IParamContext.KEY_TASK_ID, String.valueOf(jobId));
        MDC.put(TISCollectionUtils.KEY_COLLECTION, dataxName);
        Integer allRowsApproximately = msg.getAllRowsApproximately();
        logger.info("process DataX job, dataXName:{},jobid:{},jobName:{},allrows:{}", dataxName, jobId, jobName, allRowsApproximately);

        // 查看当前任务是否正在进行中，如果已经终止则要退出
        if (!isCurrentJobProcessing(jobId)) {
            logger.warn("current job id:{} jobName:{}, dataxName:{} is not processing skipping!!", jobId, jobName, dataxName);
            return;
        }

        synchronized (DataXJobConsumer.class) {
            //exec(msg);
            CommandLine cmdLine = new CommandLine("java");
            cmdLine.addArgument("-D" + Config.KEY_DATA_DIR + "=" + Config.getDataDir().getAbsolutePath());
            cmdLine.addArgument("-D" + Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS + "=" + this.useRuntimePropEnvProps());
            cmdLine.addArgument("-D" + Config.KEY_LOG_DIR + "=" + System.getProperty(Config.KEY_LOG_DIR));
            cmdLine.addArgument("-D" + Config.KEY_RUNTIME + "=daily");
            cmdLine.addArgument("-D" + CuratorDataXTaskMessage.SYSTEM_KEY_LOGBACK_PATH_KEY + "=" + CuratorDataXTaskMessage.SYSTEM_KEY_LOGBACK_PATH_VALUE);

            for (String sysParam : this.getExtraJavaSystemPrams()) {
                cmdLine.addArgument(sysParam);
            }

            cmdLine.addArgument("-classpath");
            cmdLine.addArgument(getClasspath());
            cmdLine.addArgument(getMainClassName());
            cmdLine.addArgument(String.valueOf(jobId));
            cmdLine.addArgument(jobName);
            cmdLine.addArgument(dataxName);
            //  cmdLine.addArgument(jobPath, true);
            cmdLine.addArgument(getIncrStateCollectAddress());
            // 执行模式
            cmdLine.addArgument(getExecMode().literia);
            // 估计 总记录数目
            cmdLine.addArgument(String.valueOf(allRowsApproximately));

            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getWorkingDirectory());

            executor.setStreamHandler(new PumpStreamHandler(System.out));
            executor.setExitValue(0);
            executor.setWatchdog(watchdog);
            String command = Arrays.stream(cmdLine.toStrings()).collect(Collectors.joining(" "));
            logger.info("command:{}", command);
            executor.execute(cmdLine, resultHandler);

            runningTask.computeIfAbsent(jobId, (id) -> executor.getWatchdog());
            try {
                // 等待5个小时
                resultHandler.waitFor(5 * 60 * 60 * 1000);

                if (resultHandler.hasResult()
                        && resultHandler.getExitValue() != 0
                        && resultHandler.getExitValue() != DataxExecutor.DATAX_THREAD_PROCESSING_CANCAL_EXITCODE) {
                    // it was killed on purpose by the watchdog
                    if (resultHandler.getException() != null) {
                        // logger.error("dataX:" + dataxName, resultHandler.getException());
                        throw new RuntimeException(command, resultHandler.getException());
                    }
                }
            } finally {
                runningTask.remove(jobId);
            }
        }
    }

    protected boolean isCurrentJobProcessing(Integer jobId) {
        return true;
    }

    protected abstract DataXJobSubmit.InstanceType getExecMode();

    protected abstract String getClasspath();

    protected boolean useRuntimePropEnvProps() {
        return true;
    }

    protected String[] getExtraJavaSystemPrams() {
        return new String[0];
    }

    /**
     * @return
     */
    protected abstract String getMainClassName();

    /**
     * @return
     */
    protected abstract File getWorkingDirectory();


    /**
     * Assemble 日志收集器地址
     *
     * @return
     */
    protected abstract String getIncrStateCollectAddress();

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        logger.warn("curator stateChanged to new Status:" + connectionState);
    }
}