package co.buybuddy.android.http;

import java.util.HashMap;
import java.util.Map;

class ParameterMap {

    private Map<String, Object> map;

    ParameterMap(){
        map = new HashMap<>();
    }

    public ParameterMap add(String key, Object value){
        map.put(key, value);
        return this;
    }

    public void remove(String key){
        map.remove(key);
    }

    Map<String, Object> getMap(){
        return map;
    }

}
