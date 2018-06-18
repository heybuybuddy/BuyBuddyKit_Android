package co.buybuddy.sampledelegateapp.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import co.buybuddy.sampledelegateapp.R;
import de.hdodenhof.circleimageview.CircleImageView;

import android.support.*;

/**
 * Created by furkan on 10/5/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class HitagViewHolder extends RecyclerView.ViewHolder {
    protected ImageView hitag;
    protected CircleImageView hitagLight;


    public HitagViewHolder(View v) {
        super(v);
        hitag = v.findViewById(R.id.hitagView);
        hitagLight = v.findViewById(R.id.hitagLightView);
    }
}