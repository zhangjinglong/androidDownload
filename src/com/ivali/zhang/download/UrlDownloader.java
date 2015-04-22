package com.ivali.zhang.download;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import config.ManifestInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class UrlDownloader {

	public interface OnDownloadProgressListener{
		public void onDownaloadProgress(UrlDownloader d,double progress);
		public void onDownloadFinished(UrlDownloader d);
	}

	public interface UrlDownloaderDelegate{
		public boolean shouldCancel();
	}

	
	static private final int MESSAGE_PROGRESS_WORKING = 0;
	static private final int MESSAGE_PROGRESS_FINISHED = 1;
	static private final int CONNECT_TIMEOUT = 1000 * 15;
	static private final long MESSAGE_PROGRESS_TIMEOUT = 1000;

	protected URL url = null;
	private static final int BUFFER_SIZE = 8192;
	private OnDownloadProgressListener dListener = null;
	private UrlDownloaderDelegate delegate = null;

	private long totalSize = 0;
	private long downloadSize = 0;
	private boolean finished = false;
	private int responseCode = 0;
	private boolean isOK = true;
	private String errMessage = null;
	private long lastModified = 0;

	protected Handler mHandler = null;
	protected byte[] postData=null;
	static private final String defaultPostContentType = "application/x-www-form-urlencoded";
	private String postContentType = defaultPostContentType;
	static private String userAgentString = null;
	static private HashMap<String,List<String>> hostCookies = new HashMap<String,List<String>>();

	private long actionTime = -1;
	protected long lastProgressingTime = 0;

	static {
		HttpURLConnection.setFollowRedirects(true);
	}
	
	public static String simpleGet(String url)
	{
		return simpleGet(url,null,null);
	}

	public static String simpleGet(String url,String postData,String contentType){
		String ret = null;
		try {
			UrlDownloader ud = new UrlDownloader(url);
			ud.setPostData(postData);
			ud.setPostContentType(contentType);
			ret =  ud.get();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static byte[] simpleGetBytes(String url)
	{
		return simpleGetBytes(url,null,null);
	}

	public static byte[] simpleGetBytes(String url,String postData,String contentType){
		byte[] ret = null;
		try {
			UrlDownloader ud = new UrlDownloader(url);
			ud.setPostData(postData);
			ud.setPostContentType(contentType);
			ret =  ud.getBytes();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static byte[] simpleGetBytes(String url,String postData){
		return simpleGetBytes(url,postData,defaultPostContentType);
	}

	public UrlDownloader(String _url) throws MalformedURLException {
		this.url = new URL(_url);
	}

	protected void doMessage(Message msg) {
		if(dListener!=null){
			dListener.onDownaloadProgress(this, totalSize>0?downloadSize*1.0/totalSize:-1.0);
			if(this.isFinished() && msg.what == MESSAGE_PROGRESS_FINISHED){
				dListener.onDownloadFinished(this);
			}
		}
	}

	public long getTotalSize(){
		return totalSize;
	}

	public long getDownloadedSize() 
	{
		return downloadSize;
	}
	
	public String get() throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		get(bos,false);
		String ret = bos.toString();
		bos.close();
		return ret;
	}

	public byte[] getBytes(){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] ret = null;
		try {
			get(bos,false);
			ret = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(bos!=null)bos.close();
			} catch (Exception e) {
			}
		}
		return ret;
	}
	
	public URLConnection getConnection(){
		URLConnection con = null;

		setFinished(false);
		isOK = false;
		String host = null;
		List<String> cookies = null;
		try{
			host = url.getHost();
			con = url.openConnection();
			con.setUseCaches(true);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}

		HttpURLConnection httpcon = null;
		try{
    		if(con instanceof HttpURLConnection){
    			httpcon = (HttpURLConnection)con;
    			httpcon.setInstanceFollowRedirects(true);
    			if(lastModified >0){
    				httpcon.setIfModifiedSince(lastModified);
    			}
    			if(mRequestProperty.size()>0){
    				for(Entry<String, String> ii:mRequestProperty.entrySet()){
    					httpcon.setRequestProperty(ii.getKey(),ii.getValue());
    				}
    			}
    			if(postData !=null && postData.length >0){
    				httpcon.setRequestMethod("POST");
    				httpcon.setRequestProperty("Content-Length",""+postData.length);
    				con.setDoOutput(true);
    			}
    			if(postContentType!=null && postContentType.length()>0){
    				httpcon.setRequestProperty("Content-type",postContentType);
    			}else{
    				httpcon.setRequestProperty("Content-type",defaultPostContentType);
    			}
    			
    			synchronized(hostCookies){
    				cookies = hostCookies.get(host);
    				if(cookies!=null && cookies.size()>0){
    					for (String cookie : cookies) {
    						httpcon.addRequestProperty("Cookie",cookie.split(";", 2)[0]);
    					}
    				}
    			}
    		}
			con.setRequestProperty("User-Agent", UserAgent());
			con.setConnectTimeout(CONNECT_TIMEOUT);
			con.connect();
			if(httpcon!=null){
				if(postData !=null && postData.length >0){
					OutputStream os = httpcon.getOutputStream();
					os.write(postData);
					os.flush();
					os.close();
					postData = null;
				}
				responseCode = httpcon.getResponseCode();
				if(responseCode >=400){
					isOK = false;
					con = null;
				}else{
					isOK = true;
				}
				errMessage = httpcon.getResponseMessage();
				synchronized(hostCookies){
					cookies = httpcon.getHeaderFields().get("Set-Cookie");
					if(cookies!=null && cookies.size()>0){
						hostCookies.put(host, cookies);
					}
				}
			}
		}catch(UnknownHostException e0){
		    
		}catch(Exception e){
			e.printStackTrace();
			con = null;
		}
		///Log.e(getClass().toString(),"con:"+con);
		return con;
	}

	public long get(URLConnection con,OutputStream outputstream,boolean closeOutputStream,long connectAt){

	    try{
    		if(isOK && con!=null){
    			byte[] buf = new byte[BUFFER_SIZE];
    			totalSize = con.getContentLength();
    			BufferedInputStream bis = null;
    			int n = 0;
    			isOK = false;
    			long nowtime = 0;
    			lastProgressingTime = 0;
    
    
                bis = new BufferedInputStream(con.getInputStream());
    
    			while((n = bis.read(buf)) != -1){
    				outputstream.write(buf, 0, n);
    				downloadSize+=n;
    				nowtime = System.currentTimeMillis();
    				if(dListener!=null &&(totalSize == downloadSize || lastProgressingTime == 0 || nowtime-lastProgressingTime >= MESSAGE_PROGRESS_TIMEOUT)){
    					mHandler.sendEmptyMessage(MESSAGE_PROGRESS_WORKING);
    					lastProgressingTime = nowtime;
    				}
    				if(delegate!=null){
    					if(delegate.shouldCancel()){
    						isOK=false;
    						break;
    					}
    				}
    			}
    			bis.close();
    			outputstream.flush();
    			isOK = true;
    			buf = null;
    		}
    		if(closeOutputStream){
    			outputstream.close();
    		}
		
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		setFinished(true);
		if(connectAt>0)actionTime = (System.currentTimeMillis()-connectAt);
		if(dListener!=null)mHandler.sendEmptyMessage(MESSAGE_PROGRESS_FINISHED);
		return downloadSize;
	}

	public long get(OutputStream outputstream,boolean closeOutputStream)
	{
		lastModified = 0;
		long at = System.currentTimeMillis();
		URLConnection con = getConnection();
		long ret = get(con,outputstream,closeOutputStream, at);
		return ret;
	}

	public long get(OutputStream outputstream) throws IOException{
		return get(outputstream,false);
	}

	public long toFile(File file,boolean toContinue) throws FileNotFoundException, IOException{
		URLConnection con = null;
		long ret = 0;
		long new_lastModified = 0;
		long length = 0;
		if(!toContinue)file.delete();
		if(file.isFile() && file.length() > 0){
			lastModified = file.lastModified();
		}
		long start = System.currentTimeMillis();
		con = getConnection();
		if(con != null){
			new_lastModified = con.getLastModified();
			//Log.i(getClass().toString(),"file:"+file.getAbsolutePath());
			//Log.i(getClass().toString(),"url:"+url+",lastModified:"+lastModified+",responseCode:"+responseCode+",new_lastModified:"+new_lastModified);
			if(new_lastModified == 0)new_lastModified = System.currentTimeMillis();
			if((new_lastModified>0 && new_lastModified - lastModified < 5000) || this.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED){
				ret = file.length();
			}else{
				///Log.e(getClass().toString(),"Geting "+url +" to "+file.getAbsolutePath());
				File t = new File(file.getAbsolutePath()+".download");
				if(!toContinue)t.delete();
				length = con.getContentLength();
				if(this.isOK){
					ret=get(con,new FileOutputStream(t),true,start);
					t.renameTo(file);
					t.setLastModified(new_lastModified);
					file.setLastModified(new_lastModified);
					file.setLastModified(new_lastModified);
				}else{
					t.delete();
				}
				if(isOK && length>0){
					isOK = (length == ret);
				}
			}
		}else{
			isOK = false;
		}
		if(con != null && con instanceof HttpURLConnection){
			((HttpURLConnection) con).disconnect();
		}
		return ret;
	}

	public long toFile(String file) throws IOException{
		return toFile(new File(file),true);
	}

	private File _toFile = null;
	public void startToFile(File toFile,final boolean toContinue){
		_toFile = toFile;
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					toFile(_toFile,toContinue);
				}catch (Exception e) {
					isOK = false;
					errMessage = e.getLocalizedMessage();
					e.printStackTrace();
					setFinished(true);
					if(dListener!=null)mHandler.sendEmptyMessage(MESSAGE_PROGRESS_FINISHED);
					e.printStackTrace();
				}
			}}).start();
	}
	
	public void startToFile(String toFile){
		startToFile(new File(toFile),true);
	}

	public OnDownloadProgressListener getOnDownloadProgressListener() {
		return dListener;
	}

	public synchronized void setOnDownloadProgressListener(OnDownloadProgressListener dListener) {
		this.dListener = dListener;
		if(mHandler == null){
			mHandler = new Handler(Looper.getMainLooper()) {
			        @Override  
			        public void handleMessage(Message msg) {
			        	doMessage(msg);
			        }
				};
		}
	}

	public boolean isFinished() {
		return finished;
	}

	private void setFinished(boolean finished) {
		this.finished = finished;
	}

	public String getPostData() {
		return new String(postData);
	}

	public void setPostData(String postData) {
		setPostContentType(defaultPostContentType);
		this.postData = postData.getBytes();
	}

	public UrlDownloader addPostData(String key,String value) {
		setPostContentType(defaultPostContentType);
		try {
			String s = key+"="+URLEncoder.encode(value, "UTF-8");
			if(postData !=null && postData.length >0){
				String t = new String(postData);
				if(!t.contains("=")){
					t+="=";
				}
				t += "&"+s;
				postData = t.getBytes();
			}else{
				postData = s.getBytes();
			}
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		}
		return this;
	}

	public String getPostContentType() {
		return postContentType;
	}

	public void setPostContentType(String postContentType) {
		this.postContentType = postContentType;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getErrMessage() {
		return errMessage;
	}

	public boolean isOK() {
		return this.isOK;
	}

	public UrlDownloaderDelegate getDelegate() {
		return delegate;
	}

	public void setDelegate(UrlDownloaderDelegate delegate) {
		this.delegate = delegate;
	}

	private HashMap<String,String> mRequestProperty = new HashMap<String,String>();
	
	public void setRequestProperty(String key,String value){
		mRequestProperty.put(key, value);
	}
	
	public void post(final Runnable r)
	{
		if(mHandler!=null)mHandler.post(r);
	}
	
	public void postDelayed(final Runnable r,long t)
	{
		if(mHandler!=null)mHandler.postDelayed(r,t);
	}
	
	public static String UserAgent() {
		if(userAgentString == null){
			String s = "Linux; Android/"+Build.VERSION.RELEASE+String.format("(%d)", Build.VERSION.SDK_INT);
			s += String.format("; Model/%s",Build.MODEL);
			s += String.format("; %s/%s(%d)",
					ManifestInfo.packageName,
					ManifestInfo.versionName,
					ManifestInfo.versionCode);
			userAgentString = s;
		}
		return userAgentString;
	}

	public void cleanUp() {
		mHandler = null;
		postData = null;
		this._toFile = null;
		this.errMessage = null;
		this.totalSize = 0;
		this.mRequestProperty = null;
	}

	public long getActionTime() {
		return actionTime;
	}
}
