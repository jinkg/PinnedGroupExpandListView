package com.yalin.pinnedgroupexpandlistviewdemo;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.yalin.pinnedgroupexpandlistview.PinnedGroupExpandListView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PinnedGroupExpandListView listView = (PinnedGroupExpandListView) findViewById(R.id.list_view);
        listView.setAdapter(mAdapter);
    }

    private BaseExpandableListAdapter mAdapter = new BaseExpandableListAdapter() {
        private List<GroupItem> groupItemList = Datas.getDatas();

        @Override
        public int getGroupCount() {
            return groupItemList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).childItems.size();
        }

        @Override
        public GroupItem getGroup(int groupPosition) {
            return groupItemList.get(groupPosition);
        }

        @Override
        public ChildItem getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).childItems.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this)
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
                convertView.setBackgroundColor(Color.YELLOW);
            }
            TextView textView = (TextView) convertView;
            textView.setText(getGroup(groupPosition).name);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this)
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
                convertView.setBackgroundColor(Color.WHITE);
            }
            TextView textView = (TextView) convertView;
            textView.setText(getChild(groupPosition, childPosition).name);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    };
}
