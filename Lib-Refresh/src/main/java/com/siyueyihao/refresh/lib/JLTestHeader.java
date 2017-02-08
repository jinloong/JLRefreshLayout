package com.siyueyihao.refresh.lib;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * @author zhujinlong@ichoice.com
 */

public class JLTestHeader extends FrameLayout implements JLRefreshLayout.JLRefreshHeader{
     public JLTestHeader(Context context) {
        super(context);
         TextView tv = new TextView(context);
         tv.setBackgroundColor(0xfff25555);
         tv.setText("aaaaaaa");
         addView(tv);
         //setBackgroundColor(0xfff25555);
    }

    @Override
    public void reset() {

    }

    @Override
    public void moving(int offset, int headerHeight, boolean activated) {

    }

    @Override
    public void refreshing() {

    }

    @Override
    public void refreshOver(boolean success) {

    }
}
