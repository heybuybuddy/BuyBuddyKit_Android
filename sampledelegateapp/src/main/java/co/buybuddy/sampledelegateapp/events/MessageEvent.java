package co.buybuddy.sampledelegateapp.events;

/**
 * Created by furkan on 7/20/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class MessageEvent {
    private String message;
    private String userName;

    public MessageEvent() {

    }

    public String getMessage() {
        return message;
    }

    public MessageEvent setMessage(String message) {
        this.message = message;
        return this;
    }

    public MessageEvent setUserName(String userName) {
        this.userName = userName;
        return this;
    }
}
