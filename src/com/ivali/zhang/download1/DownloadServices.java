package com.ivali.zhang.download1;

import java.io.File;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.ivali.zhang.download.MainActivity;
import com.ivali.zhang.download.R;
public class DownloadServices extends Service implements DownLoadHandler.OnDownloadProgressListener{
    private final static int DOWNLOAD_COMPLETE = -2; 
    private final static int DOWNLOAD_FAIL = -1;
     
    //自定义通知栏类
    MyNotification myNotification;
     
    String filePathString; //下载文件绝对路径(包括文件名)
  
    //通知栏跳转Intent
    private Intent updateIntent = null;
    private PendingIntent updatePendingIntent = null;
     
    private String urlpath="";
    DownLoadHandler download=null;
    private Handler updateHandler = new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case DOWNLOAD_COMPLETE:
                    //点击安装PendingIntent
                     Uri uri = Uri.fromFile(new File(filePathString));
                     Intent installIntent = new Intent(Intent.ACTION_VIEW);
                     installIntent.setDataAndType(uri, "application/vnd.android.package-archive");                     
                     updatePendingIntent = PendingIntent.getActivity(DownloadServices.this, 0, installIntent, 0);
                     myNotification.changeContentIntent(updatePendingIntent);
                     myNotification.notification.defaults=Notification.DEFAULT_SOUND;//铃声提醒                    
                    // myNotification.changeNotificationText("下载完成，请点击安装！");
                     myNotification.changeProgressStatus(100);     
                    //停止服务
                  //  myNotification.removeNotification();
                    stopSelf();
                    break;
                case DOWNLOAD_FAIL:
                    //下载失败
                    //                  myNotification.changeProgressStatus(DOWNLOAD_FAIL);  
                    myNotification.changeNotificationText("文件下载失败！");
                    stopSelf();
                    break;
                default:  //下载中
                    Log.i("service", "default"+msg.what);
        //          myNotification.changeNotificationText(msg.what+"%");
                    Log.e("TTT", "msg.what:"+msg.what);
                    myNotification.changeProgressStatus(msg.what);  
            }
        }
    };
    
    public DownloadServices() {
        // TODO Auto-generated constructor stub
    }

 
    @Override
    public void onCreate() {
        super.onCreate();
    }
 
    @Override
    public void onDestroy() {
        stopSelf();
        super.onDestroy();
    }
 
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        urlpath=intent.getStringExtra("url");
       // Log.e("TTT", "url:"+urlpath);
        if(FileUtil.getDownloadDir()==null){
        	FileUtil.createSDCardDir();
        }
        updateIntent = new Intent(this, MainActivity.class);
        PendingIntent   updatePendingIntent = PendingIntent.getActivity(this,0,updateIntent,0);
        myNotification=new MyNotification(this, updatePendingIntent, 1);
         
        //  myNotification.showDefaultNotification(R.drawable.ic_launcher, "测试", "开始下载");
       myNotification.showCustomizeNotification(R.drawable.ic_launcher, "应用下载",FileUtil.getFilename(urlpath), R.layout.notification);
         
        filePathString=FileUtil.getDownloadDir()+File.separator+FileUtil.getFilename(urlpath);
    // filePathString= Environment.getExternalStorageDirectory().getAbsolutePath() + "/DuangNet.apk";
        File file=new File(filePathString);
        //判断file
        try {
            if(file.exists() && file.isFile() &&  file.length()>0){//old file
            	file.delete();
            }
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			//Log.e("TTT", "Exception:"+Log.getStackTraceString(e));
		}
        download=new DownLoadHandler();
        download.setdListener(this);
        //获取文件
        downloadFile();
        return super.onStartCommand(intent, flags, startId);
    }
     
    public void  downloadFile(){
    	new Thread(){
    		public void run() {
    			File file=new File(filePathString);
//    			Log.e("TTT", "urlpath:"+urlpath);
//    			Log.e("TTT", "filepath:"+file.getAbsolutePath());
                file = download.getFile(urlpath, file.getAbsolutePath()); 
                if(file!=null){
                	Message msg=new Message();
            		msg.what=DOWNLOAD_COMPLETE;
            		updateHandler.sendMessage(msg);
                }else{
                	Message msg=new Message();
            		msg.what=DOWNLOAD_FAIL;
            		updateHandler.sendMessage(msg);
                }
    		};
    	}.start();
    }
    
    @Override
    @Deprecated
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
    }
 
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    int oldprogress = 0;
	@Override
	public void onDownaloadProgress(DownLoadHandler d, double progress) {
		// TODO Auto-generated method stub
		int curprogress = (int)(progress*100); 
		if (oldprogress != curprogress) {//100次  1%刷新一次
			Message msg=new Message();
			msg.what=curprogress;
			//Log.e("TTT", "curprogress:"+curprogress);
			updateHandler.sendMessage(msg);
			oldprogress = curprogress;
		}
	}

	@Override
	public void onFinished(DownLoadHandler d) {
		// TODO Auto-generated method stub
		Message msg=new Message();
		msg.what=DOWNLOAD_COMPLETE;
		updateHandler.sendMessage(msg);
	}

	@Override
	public void onDownaloadErrorMsg(DownLoadHandler d, String errormsg) {
		// TODO Auto-generated method stub
		Message msg=new Message();
		msg.what=DOWNLOAD_FAIL;
		updateHandler.sendMessage(msg);
	}
 
}
