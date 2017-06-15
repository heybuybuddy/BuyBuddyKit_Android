package co.buybuddy.android;

import java.util.Map;

/**
 * Created by furkan on 6/14/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

class HitagPassword {
    private long sale_id;
    private Map<String, String> hitag_passkeys;

    public long getSaleId() {
        return sale_id;
    }

    public String getHitagPass(String hitagId){
        if (hitag_passkeys != null && hitag_passkeys.get(hitagId) != null){
            return hitag_passkeys.get(hitagId);
        }

        return "";
    }
}
