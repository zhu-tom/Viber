package com.tomzhu.viber.ui.search;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.tomzhu.viber.models.ProtectedResult;
import com.tomzhu.viber.R;

public class SearchResultsViewAdapter extends RecyclerView.Adapter<SearchResultsViewAdapter.SearchResultsViewHolder> {
    private Object[] mDataset;
    private static final String TAG = "SearchResultsViewAdapter";

    public static class SearchResultsViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImg;
        private TextView res_username;
        private Button addBtn;
        public SearchResultsViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImg = itemView.findViewById(R.id.avatarImg);
            res_username = itemView.findViewById(R.id.res_username);
            addBtn = itemView.findViewById(R.id.add_btn);
        }

        public ImageView getAvatarImg() {
            return avatarImg;
        }

        public TextView getRes_username() {
            return res_username;
        }
    }

    public SearchResultsViewAdapter(Object[] dataSet) {
        mDataset = dataSet;
    }

    @NonNull
    @Override
    public SearchResultsViewAdapter.SearchResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout search_res = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.search_result, parent, false);
        SearchResultsViewHolder vh = new SearchResultsViewHolder(search_res);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultsViewHolder holder, int position) {
        ProtectedResult user = (ProtectedResult) mDataset[position];
        String url = user.getPhotoUrl();
        if (url != null) {
            Picasso.get().load(Uri.parse(url)).into(holder.getAvatarImg());
        } else {
            //Log.i(TAG, "Placeholder Used");
            Picasso.get().load(R.drawable.avatar_placeholder);
        }
        holder.getRes_username().setText(user.getUsername());
    }

    public void setmDataset(Object[] mDataset) {
        this.mDataset = mDataset;
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
