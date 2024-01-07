package projeto_ufu.protocol.ping;

public class PingResult {
	
	public final boolean successful;
    public final long timestamp;
    public final int MID; 

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