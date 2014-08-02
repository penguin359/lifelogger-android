/* Copied from StackOverflow answer:
   http://stackoverflow.com/questions/18403647/actionbaractivity-of-android-support-v7-appcompat-and-listactivity-in-same-act
*/

package org.northwinds.android.app;

import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class ActionBarListActivity extends ActionBarActivity {
	private ListView mListView;

	protected ListView getListView() {
		if (mListView == null) {
			mListView = (ListView) findViewById(android.R.id.list);
			mListView.setOnItemClickListener(mOnClickListener);
		}
		return mListView;
	}

	protected void setListAdapter(ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	protected ListAdapter getListAdapter() {
		ListAdapter adapter = getListView().getAdapter();
		if (adapter instanceof HeaderViewListAdapter) {
			return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
		} else {
			return adapter;
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) { }

	private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			onListItemClick((ListView) parent, v, position, id);
		}
	};
}
