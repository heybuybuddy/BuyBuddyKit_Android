package co.buybuddy.sdk.ble;

/**
 * Created by Furkan Ençkü on 9/12/17.
 * This code written by buybuddy Android Team
 */

class HitagEventFromService {

    final String hitagId;
    final HitagState event;
    int eventType = 0;

    HitagEventFromService(String hitagId, HitagState event) {
        this.hitagId = hitagId;
        this.event = event;
    }

    HitagEventFromService setEventType(int eventType) {
        this.eventType = eventType;
        return this;
    }
}
