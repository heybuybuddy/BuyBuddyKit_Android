package co.buybuddy.sampledelegateapp.adapter;

import android.graphics.Color;

/**
 * Created by furkan on 10/5/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class HitagReleaseStatus {

    private String hitagId;
    private int color;
    public int status = 0;
    public int position = 0;

    public String getHitagId() {
        return hitagId;
    }

    public void didFinish() {
        status = 10;
    }

    public HitagReleaseStatus setColor(int color) {
        this.color = color;
        return this;
    }

    public int getColor() {
        return color;
    }

    public HitagReleaseStatus setStatus(int status) {
        this.status = status;
        return this;
    }

    public HitagReleaseStatus(String hitagId, int status){
        this.hitagId = hitagId;
        this.status = status;
    }

    public HitagReleaseStatus setPosition(int position) {
        this.position = position;
        return this;
    }
}
