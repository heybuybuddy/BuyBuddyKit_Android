package co.buybuddy.sdk.model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Furkan Ençkü on 8/25/17.
 * This code written by buybuddy Android Team
 */

public final class HitagPasswordPayload {

    private ArrayList<HashMap<String, String>> hitag_passkeys;
    private long sale_id;

    public String getFirst() {
        if (hitag_passkeys != null && hitag_passkeys.get(0).get("first") != null)
            return hitag_passkeys.get(0).get("first");

        return "";
    }

    public String getSecond() {
        if (hitag_passkeys != null && hitag_passkeys.get(0).get("second") != null)
            return hitag_passkeys.get(0).get("second");

        return "";
    }

    public String getThird() {
        if (hitag_passkeys != null && hitag_passkeys.get(0).get("third") != null)
            return hitag_passkeys.get(0).get("third");

        return "";
    }
}
