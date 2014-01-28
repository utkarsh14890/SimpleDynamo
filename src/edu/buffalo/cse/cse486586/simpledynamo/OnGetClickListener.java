package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnGetClickListener implements OnClickListener{
	
	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	public OnGetClickListener(TextView _tv, ContentResolver _cr){
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
	}
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		new getter().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	private class getter extends AsyncTask<Void,String,Void>{

		@Override
		protected Void doInBackground(Void...params) {
			// TODO Auto-generated method stub
			int i = 0;
			publishProgress("Get Result:\n");
			for(i=0;i<20;i++){
				String k = Integer.toString(i);
				Cursor result = mContentResolver.query(mUri, null, k, null, null);
				if(result==null){
					publishProgress("There's Nothing in this AVD\n");
				}
				while(result.moveToNext()){
					int key = result.getColumnIndex("key");
					int val = result.getColumnIndex("value");
					publishProgress(result.getString(key)+"  "+result.getString(val)+"\n");
				}
				result.close();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return null;
		}
		protected void onProgressUpdate(String...strings){
			mTextView.append(strings[0]);
			return;
		}
		
	}
}