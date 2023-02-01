package org.apache.hadoop.fs.qinu.kodo.performance.mkdir;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.qinu.kodo.performance.QiniuKodoPerformanceBaseTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

public class MkdirLargelyConcurrentlyTest extends QiniuKodoPerformanceBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(MkdirLargelyConcurrentlyTest.class);
    private static final BlockingQueue<Path> queue = new LinkedBlockingQueue<>(10);
    private static final ExecutorService service = Executors.newCachedThreadPool();


    private long testMkdirLargely(String workDir, FileSystem fs, int dirs, int consumers) throws Exception {
        final String dir = workDir + "/testMkdirLargelyConcurrently/";
        for (int i = 0; i < consumers; i++) {
            service.submit(() -> {
                while (true) {
                    try {
                        Path e = queue.poll(2, TimeUnit.SECONDS);
                        if (e == null) {
                            // 如果超过2s收不到数据，那就退出线程
                            break;
                        }
                        fs.mkdirs(e);

                    } catch (InterruptedException | IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }

        long ms = System.currentTimeMillis();
        for (int i = 0; i < dirs; i++) {
            boolean success;
            do {
                success = queue.offer(new Path(dir + "/" + i), 1, TimeUnit.SECONDS);
            } while (!success);
        }

        return System.currentTimeMillis() - ms;
    }

    @Test
    public void testS3A() throws Exception {
        long time = testMkdirLargely(s3aTestDir, s3a, 100, 8);
        LOG.info("time: " + time);
    }

    @Test
    public void testKodo() throws Exception {
        long time = testMkdirLargely(kodoTestDir, kodo, 100, 8);
        LOG.info("time: " + time);
    }
}
