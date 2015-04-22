package com.ivali.zhang.download;

import java.io.File;
import java.net.MalformedURLException;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

public class DownloadHelper {

	static public void download(Context c,String url,String ti,String tofile)
	{
		final Context context = c.getApplicationContext();
		final int notifyid = (int) (System.currentTimeMillis()%Integer.MAX_VALUE);
		if(tofile == null){
			String fname = url.replaceAll(".*/", "").replaceAll("\\?.*", "");
			for(File f:new File[]{context.getExternalCacheDir(), context.getCacheDir()}){
				try{
					f.mkdirs();
				}catch(Exception e){}
				String d="";
				if(f!=null){
					d = f.getAbsolutePath();
				}
	            tofile = d+File.separator+fname;
	            if(f != null && f.isDirectory())break;
			}
		}
		if(tofile == null)return;
		final String title = (ti == null || ti.trim().length()<1) ?tofile.replaceAll(".*/", ""):ti;
		final String filePath = tofile;
		if(new File(filePath).exists() && new File(filePath).length()>0){
			new File(filePath).delete();
		}
		try {
			
			UrlDownloader ud = new UrlDownloader(url);
			ud.setOnDownloadProgressListener(new UrlDownloader.OnDownloadProgressListener() {
				
				@SuppressLint("NewApi")
				@Override
				public void onDownloadFinished(UrlDownloader d) {
					new File(filePath).setReadable(true, false);
					progressNotification(context,notifyid,title,null,true,d.isOK()?1.0:-1,filePath);
					if(d.isOK()){
						Misc.OpenPackageFile(context, filePath);
					}
				}
				
				@Override
				public void onDownaloadProgress(UrlDownloader d, double progress) {
					progressNotification(context,notifyid,title,null,false,progress,null);
				}
			});
			ud.startToFile(new File(filePath), false);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	static private void progressNotification(Context c,int notifyid,String t,String m,boolean isFinished,double progress,String filePath)
	{
    	Builder bd = new NotificationCompat.Builder(c)
		.setTicker(t)
		.setContentTitle(t)
		.setAutoCancel(true);
		PendingIntent pi = null;
		int prg = (int) (100*progress);
		if(isFinished){
			bd.setSmallIcon(android.R.drawable.stat_sys_download_done);
			
		    Intent intent = new Intent();
		    intent.setAction(android.content.Intent.ACTION_VIEW);
		    intent.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
		    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    pi = PendingIntent.getActivity(c, notifyid, intent, PendingIntent.FLAG_UPDATE_CURRENT
					| Intent.FLAG_ACTIVITY_NEW_TASK);
		    if(m == null)m=progress>=1.0?"下载完成":"下载失败";
		}else{
			bd.setSmallIcon(android.R.drawable.stat_sys_download);
			if(m == null)m = prg +"%";
		}
		bd.setProgress(100, prg, false);
		bd.setContentText(m);

		if(pi!=null)bd.setContentIntent(pi);
    	bd.setWhen(System.currentTimeMillis());
    	NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    	//int defaults = Notification.DEFAULT_VIBRATE;
    	//defaults |= Notification.DEFAULT_SOUND;
    	//bd.setDefaults(defaults);
    	Notification nf = bd.build();
    	if(!isFinished)nf.flags = Notification.FLAG_NO_CLEAR;
    	nm.notify(notifyid, nf);
	}
	
	static public void test(Context c)
	{
		DownloadHelper.download(c, "http://xiazai.xiazaiba.com/Android/C/cn.shuangshuangfei_54_XiaZaiBa.apk", null, null);
	}
}
