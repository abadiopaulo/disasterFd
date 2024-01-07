package projeto_ufu.principal;

import java.util.concurrent.atomic.AtomicLong;

public class ThreadState {
	
    private AtomicLong lastRun = new AtomicLong();

    public void setLastRun(long time) {
        lastRun.set(time);
    }

    public long getLastRun() {
        return lastRun.get();
    }
}
