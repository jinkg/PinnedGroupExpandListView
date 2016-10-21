package com.yalin.pinnedgroupexpandlistviewdemo;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：YaLin
 * 日期：2016/10/21.
 */

public class Datas {
    public static List<GroupItem> getDatas() {
        List<GroupItem> groupItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupItem groupItem = new GroupItem();
            groupItem.name = "Group: " + i;
            groupItem.childItems = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                ChildItem childItem = new ChildItem("ChildItem: " + (i * 10 + j));
                groupItem.childItems.add(childItem);
            }
            groupItems.add(groupItem);
        }
        return groupItems;
    }
}
