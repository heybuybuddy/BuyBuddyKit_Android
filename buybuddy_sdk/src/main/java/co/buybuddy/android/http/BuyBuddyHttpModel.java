package co.buybuddy.android.http;

/**
 * Created by furkan on 6/13/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

class BuyBuddyHttpModel {
    private String url;
    private String jsonBody;
    private HttpMethodBB method;

    public String getUrl(){
        return url != null ? url : "";
    }

    public String getJson(){ return jsonBody != null ? jsonBody : ""; }

    public HttpMethodBB getMethodType(){ return method != null ? method : HttpMethodBB.GET; }

    public BuyBuddyHttpModel(String url, String jsonBody, HttpMethodBB method){
        this.url = url;
        this.jsonBody = jsonBody;
        this.method = method;
    }
}

