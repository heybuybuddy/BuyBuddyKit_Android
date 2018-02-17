package co.buybuddy.sampledelegateapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import co.buybuddy.sampledelegateapp.R;

/**
 * Created by furkan on 10/5/17.
 * Gururla sunar. AHAHAHAHA Some spagetties
 */

public class HitagReleaseStatusAdapter extends RecyclerView.Adapter<HitagViewHolder> {

    private Context ctx;
    public List<HitagReleaseStatus> hitagStates = new ArrayList<>();

    public void clear() {
        hitagStates.clear();
        this.notifyDataSetChanged();
    }

    public int findPositionWith(String hitagId) {
        int position = -1;
        for(int i = 0; i < hitagStates.size(); i++) {
            if (hitagId.equals(hitagStates.get(i).getHitagId())) {
                position = i;
            }
        }

        return position;
    }

    public HitagReleaseStatusAdapter(Context context) {
        this.ctx = context;
    }

    public void updateHitag(String hitagId, int status, int color) {

        int position = -1;
        for(int i = 0; i < hitagStates.size(); i++) {
            if (hitagId.equals(hitagStates.get(i).getHitagId())) {
                position = i;
            }
        }

        if (position != -1) {
            hitagStates.get(position).setStatus(status).setColor(color);
            this.notifyItemChanged(position);
        } else {
            hitagStates.add(new HitagReleaseStatus(hitagId, status)
                                .setColor(color)
                                .setPosition(hitagStates.size()));
            this.notifyItemInserted(hitagStates.size());
        }
    }


    @Override
    public HitagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.row_hitag, parent, false);

        return new HitagViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final HitagViewHolder holder, int position) {
        hitagStates.get(position).setPosition(holder.getAdapterPosition());

        HitagReleaseStatus hStatus = hitagStates.get(position);
        hitagStates.get(position).position = holder.getLayoutPosition();

        holder.hitagLight.setImageDrawable(new ColorDrawable(hStatus.getColor()));
    }

    public void animateLights(HitagViewHolder holder, int color) {

        holder.hitagLight.setImageDrawable(new ColorDrawable(color));
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(holder.hitagLight, "alpha",  1f, .3f);
        fadeOut.setDuration(1000);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(holder.hitagLight, "alpha", .3f, 1f);
        fadeIn.setDuration(1000);
        ObjectAnimator fadeOut2 = ObjectAnimator.ofFloat(holder.hitagLight, "alpha",  1f, .3f);
        fadeOut2.setStartDelay(1000);
        fadeOut2.setDuration(1000);
        ObjectAnimator fadeIn2 = ObjectAnimator.ofFloat(holder.hitagLight, "alpha", .3f, 1f);
        fadeIn2.setStartDelay(2000);
        fadeIn2.setDuration(1000);

        final AnimatorSet mAnimationSet = new AnimatorSet();

        mAnimationSet.play(fadeOut).after(fadeIn).after(fadeOut2).after(fadeIn2);
        mAnimationSet.start();
    }

    @Override
    public int getItemCount() {
        return hitagStates.size();
    }
}
