package co.buybuddy.sdk;

import java.util.Map;

/**
 * Created by Furkan Ençkü on 6/14/17.
 * This code written by buybuddy Android Team
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
