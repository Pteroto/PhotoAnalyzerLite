package com.example.android.tflitecamerademo;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by gustavomonteiro on 28/08/17.
 */

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
    private List<Bitmap> mPhotos;
    private List<List<String>> mResults;
    private Context mContext;


    public ResultAdapter(List<Bitmap> photos, List<List<String>> results, Context context) {
        mPhotos = photos;
        mResults = results;
        mContext = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mImageView.setImageBitmap(mPhotos.get(position));
        //Glide.with(mContext).load(mPhotos.get(position)).into(holder.mImageView);
        holder.mTextView1.setText(mResults.get(position).get(0));
        if (mResults.get(position).size() > 1) {
            holder.mTextView2.setText(mResults.get(position).get(1));
        } else {
            holder.mTextView2.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return mPhotos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImageView;
        public TextView mTextView1;
        public TextView mTextView2;

        public ViewHolder(View itemView) {
            super(itemView);

            mImageView = (ImageView) itemView.findViewById(R.id.image);
            mTextView1 = (TextView) itemView.findViewById(R.id.text1);
            mTextView2 = (TextView) itemView.findViewById(R.id.text2);
        }
    }

}
