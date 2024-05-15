package com.cj.utils;

import com.cj.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @Classname ProcessUtils
 * @Description 什么也没有写哦~
 * @Date 2024/3/16 12:31
 * @Created by 憧憬
 */
public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    public static String executeCommand(String cmd, Boolean outprintLog) {
        if (StringTools.isEmpty(cmd)) {
            logger.error("--- 执行命令失败，cmd指令为空");
            return null;
        }

        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec(cmd); // 执行cmd命令
            // 执行ffmpeg指令
            // 取出输出流和错误流信息
            // 注意：必须要取出ffmpeg在执行命令过程中产生的输出信息，如果取不出的话 当输出信息填满jvm存储信息输出流信息的缓冲区时候，线程就会阻塞
            PrintStream errorStream = new PrintStream(process.getErrorStream());
            PrintStream inputStream = new PrintStream(process.getInputStream());
            errorStream.start();
            inputStream.start();
            // 等待cmd命令执行完毕
            process.waitFor();
            // 获取执行结果字符串
            String result = errorStream.stringBuffer.append(inputStream.stringBuffer + "\n").toString();
            // 输出执行命令信息
            if(outprintLog){
                logger.info("执行命令：{}，已完毕 输出信息：{}", cmd, result);
            }else {
                logger.info("执行命令：{} 已完成", cmd);
            }
            return result;
        } catch (Exception e) {
            logger.error("执行命令失败");
            throw new BusinessException("执行命令失败");
        }finally {
            if(null != process){
                ProcessKiller processKiller = new ProcessKiller(process);
                runtime.addShutdownHook(processKiller);
            }
        }
    }

    /**
     * 程序推出前关闭cmd线程
     */
    static class ProcessKiller extends Thread{
        private Process process;
        public ProcessKiller(){

        }
        public ProcessKiller(Process process){
            this.process = process;
        }

        @Override
        public void run() {
            this.process.destroy();
        }
    }

    /**
     * 用于取出cmd线程执行过程中产生的各种输出流
     */
    static class PrintStream extends Thread {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuffer stringBuffer = new StringBuffer();

        public PrintStream() {

        }

        public PrintStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }


        @Override
        public void run() {
            if (null == inputStream){
                return;
            }
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                while((line = bufferedReader.readLine()) != null){
                    stringBuffer.append(line);
                }
            }catch (Exception e){
                logger.error("读取输出流错误，错误信息:{}", e.getMessage());
            }finally {
                if(null != bufferedReader){
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(null != inputStream){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
