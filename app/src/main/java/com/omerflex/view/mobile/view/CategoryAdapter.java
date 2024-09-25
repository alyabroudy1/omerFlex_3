package com.omerflex.view.mobile.view;

// CategoryAdapter.java

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omerflex.R;
import com.omerflex.view.mobile.MobileHomepageActivity;
import com.omerflex.view.mobile.entity.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private Context context;
    private List<Category> categoryList;

    private OnMovieClickListener onMovieClickListener;

    public CategoryAdapter(Context context, List<Category> categoryList, OnMovieClickListener onMovieClickListener) {
        this.context = context;
        this.categoryList = categoryList;
        if (onMovieClickListener instanceof MobileHomepageActivity.MovieItemClickListener){
            ((MobileHomepageActivity.MovieItemClickListener) onMovieClickListener).setCategoryAdapter(this);
        }
        this.onMovieClickListener = onMovieClickListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.mobile_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.categoryNameTextView.setText(category.getCategoryName());

        // Set up the horizontal RecyclerView for movies
        HorizontalMovieAdapter movieAdapter = new HorizontalMovieAdapter(context, category.getMovieList(), onMovieClickListener);
        holder.horizontalRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.horizontalRecyclerView.setAdapter(movieAdapter);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }


    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameTextView;
        RecyclerView horizontalRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.categoryNameTextView);
            horizontalRecyclerView = itemView.findViewById(R.id.horizontalRecyclerView);
        }
    }
}
