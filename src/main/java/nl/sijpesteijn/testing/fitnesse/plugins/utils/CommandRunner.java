package nl.sijpesteijn.testing.fitnesse.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Start a process and monitor the std out and std error.
 * 
 */
public class CommandRunner {
    private Process process;
    private final StringBuilder errorBuffer = new StringBuilder();
    private final StringBuilder outputBuffer = new StringBuilder();
    private final StringBuilder inputBuffer = new StringBuilder();
    private int exitValue;
    private File workDirectory = null;

    public CommandRunner(final String workDirectory) {
        if (workDirectory != null)
            this.workDirectory = new File(workDirectory);
    }

    /**
     * Run the command.
     * 
     * @param command
     *        {@link java.lang.String}
     * @throws IOException
     * @throws InterruptedException
     */
    public void start(final String command, final boolean waitForProcessToFinish, final String endCondition)
            throws IOException, InterruptedException
    {
        process = Runtime.getRuntime().exec(command, null, workDirectory);
        if (waitForProcessToFinish) {
            waitForSetupToFinish(endCondition);
        }
    }

    /**
     * Check if the unpacking of FitNesse has finished.
     * 
     * @param endCondition
     * 
     * @return {@link boolean}
     * @throws InterruptedException
     */
    private void waitForSetupToFinish(final String endCondition) throws InterruptedException {
        createStreamMonitors(process);
        while (true) {
            try {
                exitValue = process.exitValue();
                return;
            } catch (final IllegalThreadStateException itse) {
                // Process has not finished yet
            }
            if (endCondition != null && !endCondition.equals("")) {
                if (inputBuffer.toString().endsWith(endCondition)) {
                    return;
                }
            }
            Thread.sleep(2000);
        }
    }

    /**
     * Create the stream monitors.
     * 
     * @param process
     *        {@link java.lang.Process}
     */
    private void createStreamMonitors(final Process process) {
        final InputStream errorStream = process.getErrorStream();
        new Thread(new InputStreamToBufferMonitor(errorStream, errorBuffer)).start();
        final InputStream inputStream = process.getInputStream();
        new Thread(new InputStreamToBufferMonitor(inputStream, inputBuffer)).start();
    }

    /**
     * Check if the buffer contains the specified string.
     * 
     * @param buffer
     *        {@link java.lang.StringBuilder}
     * @param string
     *        {@link java.lang.String}
     * @return {@link boolean}
     */
    private boolean bufferContains(final StringBuilder buffer, final String string) {
        return buffer.toString().indexOf(string) > 0;
    }

    /**
     * Is the error buffer empty?
     * 
     * @return {@link boolean}
     */
    public boolean errorBufferHasContent() {
        return errorBuffer.length() > 0;
    }

    /**
     * Check if the error buffer contains the specified string.
     * 
     * @param string
     *        {@link java.lang.String}
     * @return {@link boolean}
     */
    public boolean errorBufferContains(final String string) {
        return bufferContains(errorBuffer, string);
    }

    /**
     * Get the error buffer contents.
     * 
     * @return {@link java.lang.CharSequence}
     */
    public CharSequence getErrorBuffer() {
        return errorBuffer;
    }

    /**
     * Return the output buffer contents.
     * 
     * @return {@link java.lang.CharSequence}
     */
    public CharSequence getOutputBuffer() {
        return outputBuffer;
    }

    public int getExitValue() {
        return exitValue;
    }
}
