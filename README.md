# PinnedGroupExpandListView

<img src="https://github.com/jinkg/Screenshots/blob/master/PinnedGroupExpandListView/pinned_group_expand_listview.gif" width="180" height="320">

## Usage

```xml
 <com.yalin.pinnedgroupexpandlistview.PinnedGroupExpandListView
        android:id="@+id/list_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:childDivider="@drawable/list_divider"
        android:divider="@drawable/list_divider"
        android:dividerHeight="1px"
        android:groupIndicator="@null" />
```

```java
 PinnedGroupExpandListView listView = (PinnedGroupExpandListView) findViewById(R.id.list_view);
 listView.setAdapter(mAdapter);
```

You can see a complete usage in the demo app.

## Feedback

nilaynij@gmail.com.
