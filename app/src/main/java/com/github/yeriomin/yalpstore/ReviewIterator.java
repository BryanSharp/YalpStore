package com.github.yeriomin.yalpstore;

import android.content.Context;

import com.github.yeriomin.yalpstore.model.Review;

import java.util.Iterator;
import java.util.List;

abstract public class ReviewIterator implements Iterator<List<Review>> {

    protected String packageName;
    protected Context context;

    protected int page = -1;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
