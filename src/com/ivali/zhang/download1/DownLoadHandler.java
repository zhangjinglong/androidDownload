package com.ivali.zhang.download1;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownLoadHandler 
{
	private OnDownloadProgressListener dListener = null;
	protected Handler mHandler = null;
	private long totalSize = 0;
	private long downloadSize = 0;
	private boolean finished = false;
	static private final int MESSAGE_PROGRESS_WORKING = 0;
	static private final int MESSAGE_PROGRESS_FINISHED = 1;
	static private final int MESSAGE_PROGRESS_ERRORMESSAGE = 2;
	private String errormsg="";
	public long getTotalSize(){
		return totalSize;
	}

	public long getDownloadedSize() 
	{
		return downloadSize;
	}
    public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public OnDownloadProgressListener getdListener() {
		return dListener;
	}

	public void setdListener(OnDownloadProgressListener dListener) {
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
	protected void doMessage(Message msg) {
		if(dListener!=null){
//			Log.e("TTT", "totalSize:"+totalSize);
//			Log.e("TTT", "downloadSize:"+downloadSize);
			dListener.onDownaloadProgress(this, totalSize>0?downloadSize*1.0/totalSize:-1.0);
			if(this.isFinished() && msg.what == MESSAGE_PROGRESS_FINISHED){
				dListener.onFinished(this);
			}
			if(msg.what == MESSAGE_PROGRESS_ERRORMESSAGE){
				dListener.onDownaloadErrorMsg(this, errormsg);
			}
		}
	}
	public  File getFile(String urlpath, String filepath) 
    {
        try 
        {
            URL url = new URL(urlpath);
            File file = new File(filepath);
            FileOutputStream fos = new FileOutputStream(file);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          
            //下载的请求是GET方式，conn的默认方式也是GET请求
            conn.setRequestMethod("GET");      
            //服务端的响应的时间
            conn.setConnectTimeout(5000);
            totalSize = conn.getContentLength();
            //获取到要下载的apk的文件的输入流
            InputStream is = conn.getInputStream();
            if(conn.getResponseCode()>=400){
            	errormsg = conn.getResponseMessage();
				if(dListener!=null){
					mHandler.sendEmptyMessage(MESSAGE_PROGRESS_ERRORMESSAGE);
				}
            }
            //设置一个缓存区
            byte[] buffer = new byte[1024*8];
            
            int len = 0;
            while ((len = is.read(buffer)) != -1) 
            {
                fos.write(buffer, 0, len);
                downloadSize+=len;
				if(dListener!=null){
					mHandler.sendEmptyMessage(MESSAGE_PROGRESS_WORKING);
				}
                
                //设置睡眠时间，便于我们观察下载进度
                Thread.sleep(30);
            }
            
            //刷新缓存数据到文件中
            fos.flush();
            //关流
            fos.close();
            is.close();
    		setFinished(true);
    		if(dListener!=null)mHandler.sendEmptyMessage(MESSAGE_PROGRESS_FINISHED);
            return file;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return null;
        }
    }

    //获取文件名，如qhbao.apk
    public  String getFilename(String urlpath) 
    {
        return urlpath.substring(urlpath.lastIndexOf("/") + 1, urlpath.length());
    }
    //下载监听接口
	public interface OnDownloadProgressListener{
		public void onDownaloadProgress(DownLoadHandler d,double progress);
		public void onFinished(DownLoadHandler d);
		public void onDownaloadErrorMsg(DownLoadHandler d,String errormsg);
	}
    
}
