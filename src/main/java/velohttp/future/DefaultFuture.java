package velohttp.future;


/**
 * 参考  io.netty.util.concurrent.DefaultPromise
 * @param <V>
 */
public class DefaultFuture<V> extends AbstractFuture<V> {

    private V result;

    public void setResult(V result) {
        this.result = result;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void await() throws InterruptedException {
        if (isDone()) {
            return ;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        synchronized (this) {
            while (!isDone()) {
//            Thread.sleep(10);
                wait();
            }
        }

//        synchronized (this) {
//            while (!isDone()) {
//                wait(10);
//            }
//        }
    }

    @Override
    public boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (isDone()) {
            return true;
        }

        if (timeoutNanos <= 0) {
            return isDone();
        }

        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;
        try {
            for (;;) {
                synchronized (this) {
                    if (isDone()) {
                        return true;
                    }
                    try {
                        wait(waitTime / 1000000, (int) (waitTime % 1000000));
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        } else {
                            interrupted = true;
                        }
                    } finally {
                    }
                }
                if (isDone()) {
                    return true;
                } else {
                    waitTime = timeoutNanos - (System.nanoTime() - startTime);
                    if (waitTime <= 0) {
                        return isDone();
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public V getNow() {
        return result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isDone() {
        return result != null;
    }

}
