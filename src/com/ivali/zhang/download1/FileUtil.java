package com.ivali.zhang.download1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class FileUtil {
	// 在SD卡上创建一个文件夹
	public static void createSDCardDir() {

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			// 创建一个文件夹对象，赋值为外部存储器的目录
			File sdcardDir = Environment.getExternalStorageDirectory();
			// 得到一个路径，内容是sdcard的文件夹路径和名字
			String path = sdcardDir.getPath() + "/apkdownload";
			File path1 = new File(path);
			if (!path1.exists()) {
				// 若不存在，创建目录，可以在应用启动的时候创建
				path1.mkdirs();
			}
		} else {
			return;
		}
	}

	public static String getDownloadDir() {

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			// 创建一个文件夹对象，赋值为外部存储器的目录
			File sdcardDir = Environment.getExternalStorageDirectory();
			// 得到一个路径，内容是sdcard的文件夹路径和名字
			String path = sdcardDir.getPath() + "/apkdownload";
			return path;
		} else {
			return null;
		}
	}
    //获取文件名，如qhbao.apk
    public static  String getFilename(String urlpath) 
    {
        return urlpath.substring(urlpath.lastIndexOf("/") + 1, urlpath.length());
    }
	// 安装apk
	public static void installAPK(Context mContext, File file) {
		// 隐式意图
		Intent intent = new Intent();
		// 设置意图的动作
		intent.setAction("android.intent.action.VIEW");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// 为意图添加额外的数据
		intent.addCategory("android.intent.category.DEFAULT");
		// 设置意图的数据与类型
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");
		// 激活该意图
		mContext.startActivity(intent);
	}

	// 通过报名检索应用是否安装
	public static boolean isAvilible(Context context, String packageName) {
		final PackageManager packageManager = context.getPackageManager();// 获取packagemanager
		List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
		List<String> pName = new ArrayList<String>();// 用于存储所有已安装程序的包名
		// 从pinfo中将包名字逐一取出，压入pName list中
		if (pinfo != null) {
			for (int i = 0; i < pinfo.size(); i++) {
				String pn = pinfo.get(i).packageName;
				pName.add(pn);
			}

		}
		return pName.contains(packageName);// 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
	}

	// 通过文件名检索应用是否已经下载
	public static boolean isExist(Context context, String apkname) {
		List<String> filenames = new ArrayList<String>();// 用于存储所有已安装程序的包名
		createSDCardDir();
		File parentfile = new File(getDownloadDir());
		if (parentfile.isDirectory()) {
			File[] files = parentfile.listFiles();
			for (int i = 0; i < files.length; i++) {
				String filename = files[i].getName();
				// Log.e("TTT", filename);
				filenames.add(filename);
			}
		}
		return filenames.contains(apkname);// 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
	}

	// 获取size
	public static long getSize(Context context, String apkname) {
		File file = new File(getDownloadDir() + "/" + apkname);

		return file.length();
	}

	// 通过包名检索应用是否需要升级
	public static boolean isUpdate(Context context, String packagename,
			long fuwuversion) {
		long bendiVersion = -1;
		try {
			bendiVersion = context.getPackageManager().getPackageInfo(
					packagename, 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fuwuversion > bendiVersion ? true : false;// 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
	}
	//通过安装包的路径获取安装包的versioncode
	public static long getVersionCodeforPath(Context context,String archiveFilePath){
		PackageManager pm = context.getPackageManager();    
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES); 
        int versioncode=-9999;
        if(info != null){    
            ApplicationInfo appInfo = info.applicationInfo;    
            versioncode=info.versionCode;       //得到版本号  
        }
        if(versioncode==-9999){
        	return -9999;
        }else{
        	return versioncode;
        }
	}
}
