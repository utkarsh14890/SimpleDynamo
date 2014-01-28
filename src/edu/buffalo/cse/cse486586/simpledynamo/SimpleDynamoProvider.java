package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	public static final String path = "/data/data/edu.buffalo.cse.cse486586.simpledynamo/files";
	final String KEY_FIELD = "key";
    final String VALUE_FIELD = "value";
	String[] ColumnNames = new String[]{"key","value"};
	fileWriter fw = new fileWriter(path);
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		TelephonyManager tel = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
		final String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		String[] keyval = new String[2];
		keyval[0] = values.getAsString(KEY_FIELD);
		keyval[1] = values.getAsString(VALUE_FIELD);
		new sender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, portStr,keyval[0],keyval[1]);
		fw.write(keyval);
		return uri;
	}

	@Override
	public boolean onCreate() {
		TelephonyManager tel = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
		final String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		new listener().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, portStr);
		new updater().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, portStr);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		fileReader fr = new fileReader();
		MatrixCursor mc = null;
		if(selection.equals("local")){
			mc = fr.read();
		}
		else{
			mc = fr.readSingle(selection);
		}
		return mc;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    private class fileWriter{
		final String path;
		String key,val;
		/*fileWriter(String path,String[] keyval){
			this.path = path;
			key = keyval[0];
			val = keyval[1];
		}*/
		fileWriter(String path){
			this.path = path;
		}
		void write(String[] keyval){
			key = keyval[0];
			val = keyval[1];
			File file = new File(path,key);
			FileWriter fw = null;
			File f = new File(path);
			if(!f.exists())
				f.mkdirs();
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try{
				fw = new FileWriter(file.getAbsoluteFile());
			}
			catch(Exception e){
				Log.e("inserterror", "can't open the file", e);
			}
			BufferedWriter out = new BufferedWriter(fw);
			try{
				out.write(val);
				out.close();
			}
			catch(Exception e){
				Log.e("inserterror", "can't write to the file", e);
			}

		}
	}
    private class fileReader{
		MatrixCursor mc = new MatrixCursor(ColumnNames);
		String[] res = new String[2];
		MatrixCursor read(){
			File f = new File(path);
			if(!f.exists()){
				Log.e("fileReader","returning null");
				return null;
			}
			if (f.listFiles()==null)
				return null;
			for(File file : f.listFiles()){
				FileReader fr = null;
				try {
					fr = new FileReader(file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				BufferedReader in = new BufferedReader(fr);
				int c;
				char[] buf = new char[128];
				StringBuffer fileData = new StringBuffer();
				try {
					while((c=in.read(buf)) != -1){
						String readData = String.valueOf(buf, 0, c);
						fileData.append(readData);
					}
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				res[0] = file.getName();
				res[1] = fileData.toString();
				mc.addRow(res);
			}
			return mc;
		}
		MatrixCursor readSingle(String key){
			File f = new File(path+"/"+key);
			if(!f.exists()){
				Log.e("fileReader","returning null");
				return null;
			}
			FileReader fr = null;
			try {
				fr = new FileReader(f);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedReader in = new BufferedReader(fr);
			int c;
			char[] buf = new char[128];
			StringBuffer fileData = new StringBuffer();
			try {
				while((c=in.read(buf)) != -1){
					String readData = String.valueOf(buf, 0, c);
					fileData.append(readData);
				}
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			res[0] = f.getName();
			res[1] = fileData.toString();
			mc.addRow(res);
			return mc;
		}
	}

    private class listener extends AsyncTask<String,Void,Void>{

		@Override
		protected Void doInBackground(String... params) {
			try {
				ServerSocket s = new ServerSocket(10000);
				Log.e(params[0],"Server Started");
				fileWriter f = new fileWriter(path);
				while(true){
					Socket socket = s.accept();
					Log.e(params[0],"Connection Accepted");
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					out.write("available\n");
					out.flush();
					//socket.setSoTimeout(100);
					String line = in.readLine();
					if(line.equals("nothing")){
						Log.e(params[0],"Nothing found");
						socket.close();
					}
					else if(line.equals("send")){
						Log.e(params[0],"Got send");
						socket.close();
						new updateValues().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params[0]);
					}
					else if(line.equals("add")){
						out.flush();
						String input;
						while((input = in.readLine())!=null){
							String[] keyval = input.split(":");
							Log.e("key",keyval[0]);
							Log.e("val",keyval[1]);
							f.write(keyval);
							//socket.close();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
    }
    private class sender extends AsyncTask<String,Void,Void>{

		@Override
		protected Void doInBackground(String... params) {
			if(params[0].equals("5554")){
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress("10.0.2.2",11112),100);
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					out.write("add\n");
					out.flush();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String str;
					if((str = in.readLine())!=null){
						if(str.equals("available")){
							Log.e(params[0],"Entered5556");
							out.print(params[1]+":"+params[2]+"\n");
							out.flush();
							socket.close();
						}
						else{
							Log.e(params[0], "Nothing available");
							socket.close();
						}
					}
					//else
					socket.close();
					Socket socket1 = new Socket();
					socket1.connect(new InetSocketAddress("10.0.2.2",11116),100);
					PrintWriter out1 = new PrintWriter(socket1.getOutputStream());
					out1.write("add\n");
					out1.flush();
					BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
					String str1;
					if((str1 = in1.readLine())!=null){
						if(str1.equals("available")){
							Log.e(params[0],"Entered5558");
							out1.print(params[1]+":"+params[2]+"\n");
							out1.flush();
							socket1.close();
						}
						else{
							Log.e(params[0], "Nothing available");
							socket1.close();
						}
					}
					socket1.close();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(params[0].equals("5556")){
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress("10.0.2.2",11108),100);
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					out.write("add\n");
					out.flush();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String str;
					if((str = in.readLine())!=null){
						if(str.equals("available")){
							Log.e(params[0],"Entered5554");
							out.print(params[1]+":"+params[2]+"\n");
							out.flush();
							socket.close();
						}
						else{
							Log.e(params[0], "Nothing available");
							socket.close();
						}
					}
					//else
					socket.close();
					Socket socket1 = new Socket();
					socket1.connect(new InetSocketAddress("10.0.2.2",11116),100);
					PrintWriter out1 = new PrintWriter(socket1.getOutputStream());
					out1.write("add\n");
					out1.flush();
					BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
					String str1;
					if((str1 = in1.readLine())!=null){
						if(str1.equals("available")){
							Log.e(params[0],"Entered5558");
							out1.print(params[1]+":"+params[2]+"\n");
							out1.flush();
							socket1.close();
						}
						else{
							Log.e(params[0], "Nothing availabe");
							socket1.close();
						}
					}
					socket1.close();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			}
			else{
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress("10.0.2.2",11108),100);
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					out.write("add\n");
					out.flush();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String str;
					if((str = in.readLine())!=null){
						if(str.equals("available")){
							Log.e(params[0],"Entered5554");
							out.print(params[1]+":"+params[2]+"\n");
							out.flush();
							socket.close();
						}
						else{
							Log.e(params[0], "Nothing available");
							socket.close();
						}
					}
					//else
					socket.close();
					Socket socket1 = new Socket();
					socket1.connect(new InetSocketAddress("10.0.2.2",11112),100);
					PrintWriter out1 = new PrintWriter(socket1.getOutputStream());
					out1.write("add\n");
					out1.flush();
					BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
					String str1;
					if((str1 = in1.readLine())!=null){
						if(str1.equals("available")){
							Log.e(params[0],"Entered5556");
							out1.print(params[1]+":"+params[2]+"\n");
							out1.flush();
							socket1.close();
						}
						else{
							Log.e(params[0], "Nothing availabe");
							socket1.close();
						}
					}
					socket1.close();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return null;
		}
    	
    }
    
    private class updater extends AsyncTask<String,Void,Void>{

		@Override
		protected Void doInBackground(String... params) {
			// TODO Auto-generated method stub
			int port;
			if(params[0].equals("5554")){
				port = 11116;		
			}
			else if(params[0].equals("5556")){
				port = 11108;
			}
			else{
				port = 11112;
			}
			try {
				Socket send = new Socket();
				send.connect(new InetSocketAddress("10.0.2.2",port),100);
				BufferedReader in = new BufferedReader(new InputStreamReader(send.getInputStream()));
				PrintWriter out = new PrintWriter(send.getOutputStream());
				String input = in.readLine();
				if(input==null){
					Log.e(params[0], "Closing Socket");
					send.close();
				}
				else{
					//Thread.sleep(1000);
					Log.e(params[0],"sending send");
					out.print("send\n");
					out.flush();
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch(NullPointerException e){
				Log.e(params[0], "caught null pointer");
			} //catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			//}
			return null;
		}
    	
    }
    private class updateValues extends AsyncTask<String,Void,Void>{

		@Override
		protected Void doInBackground(String... params) {
			// TODO Auto-generated method stub
			fileReader fr = new fileReader();
			int port;
			if(params[0].equals("5554"))
				port = 11112;
			else if(params[0].equals("5556"))
				port = 11116;
			else
				port = 11108;
			try {
				Socket sender = new Socket("10.0.2.2",port);
				PrintWriter out = new PrintWriter(sender.getOutputStream());
				MatrixCursor res = fr.read();
				if(res==null){
					out.print("nothing");
					out.flush();
				}
				else{
					out.print("add\n");
					out.flush();
					while(res.moveToNext()){
						int key = res.getColumnIndex("key");
						int val = res.getColumnIndex("value");
						out.print(res.getString(key)+":"+res.getString(val)+"\n");
						out.flush();
					}
					res.close();
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return null;
		}
    	
    }

}
