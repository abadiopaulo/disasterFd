package projeto_ufu.protocol.ping;

public class PingResult {
	
	private final boolean successful;
    private final long timestamp;
    private final int MID; 

    public PingResult(boolean successful, long timestamp, int MID) {
      
    	this.successful = successful;
        this.timestamp = timestamp;
        this.MID = MID;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getMID() {
        return MID;
    }
}