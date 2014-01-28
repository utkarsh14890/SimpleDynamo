package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnPut3ClickListener implements OnClickListener{

	private static final int TEST_CNT = 20;
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	private final ContentValues[] mContentValues;

	public OnPut3ClickListener(TextView _tv, ContentResolver _cr){
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
		mContentValues = initTestValues();
	}
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
	private ContentValues[] initTestValues() {
		ContentValues[] cv = new ContentValues[TEST_CNT];
		for (int i = 0; i < TEST_CNT; i++) {
			cv[i] = new ContentValues();
			cv[i].put(KEY_FIELD, Integer.toString(i));
			cv[i].put(VALUE_FIELD, "put" + Integer.toString(i+30));
		}

		return cv;
	}

	@Override
	public void onClick(View arg0) {
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private class Task extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (testInsert()) {
				publishProgress("Put3 success\n");
			} else {
				publishProgress("Put3 fail\n");
				return null;
			}
			return null;
		}
		protected void onProgressUpdate(String...strings) {
			mTextView.append(strings[0]);

			return;
		}

		private boolean testInsert() {
			try {
				for (int i = 0; i < TEST_CNT; i++) {
					mContentResolver.insert(mUri, mContentValues[i]);
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				Log.e("InsertError", e.toString());
				return false;
			}

			return true;
		}

	}
	
}