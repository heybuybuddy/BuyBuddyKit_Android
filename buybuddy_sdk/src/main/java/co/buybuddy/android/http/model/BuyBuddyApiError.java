package co.buybuddy.android.http.model;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class BuyBuddyApiError extends Throwable {
    private String tracemessage = "";
    private String tracecode = "";
    private int responsecode = -1;

    public String getTraceMessage(){
        return tracemessage;
    }

    public String getTraceCode(){
        return tracecode;
    }

    public int getResponseCode(){
        return responsecode;
    }

    public BuyBuddyApiError(String traceCode,
                            String traceMessage,
                            int responseCode){

        this.tracecode = traceCode;
        this.tracemessage = traceMessage;
        this.responsecode = responseCode;
    }
}
