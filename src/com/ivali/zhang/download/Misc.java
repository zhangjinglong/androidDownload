package com.ivali.zhang.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;

public class Misc {
	public static final int BUFFER_SIZE = 1024 * 8;
	public static final long OBJECT_CACHE_TIMEOUT = 1000 *60*30;

	//判断是否有网络,true只能说明网络功能已开启,不意味着一定能上网
	synchronized static public boolean hasConnection(Context c){
		if(c == null)return false;
		boolean ret = false;
		ConnectivityManager con = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ninfo = con.getActiveNetworkInfo();
		if(ninfo != null)ret = ninfo.isConnected();
		return ret;
	}

	static public boolean testToInternet(){
		return testToInternet(null);
	}
	
	//测试是否能链接到互联网,等待时间比较长需要线程中使用
	static public boolean testToInternet(String url){
		HttpURLConnection con = null;
		String [] urls = null;
		if(url == null || url.length() ==0){
			urls = new String[]{"http://www.baidu.com/"};
		}else{
			urls = new String[]{url};
		}
	
		boolean ret = false;
		for(int i=0;i<urls.length;i++){
			try {
				con = (HttpURLConnection) new URL(urls[i]).openConnection();
				con.setReadTimeout(10 * 1000);
				con.setRequestMethod("GET");
				con.setDoInput(false);
				con.setDoOutput(false);
				con.setDefaultUseCaches(false);
				con.connect();
				int code = con.getResponseCode();
				if(code == HttpURLConnection.HTTP_CLIENT_TIMEOUT ||
				   code == HttpURLConnection.HTTP_RESET
						){
					ret = false;
				}else{
					ret = true;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (ProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(con!=null){
					con.disconnect();
				}
			}
			if(ret)break;
		}
		return ret;
	}


	//读取文件内容到String
	static public String getFileContent(File f){
		byte[] buf = new byte[BUFFER_SIZE];
		String ret = null;
		BufferedInputStream bis = null;
		int n = 0;

		try {
			bis = new BufferedInputStream(new FileInputStream(f));
			while((n = bis.read(buf)) != -1){
				if(ret == null){
					ret = new String(buf,0,n);
				}else{
					ret = ret + new String(buf,0,n);
				}
			}
			bis.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	//读取文件内容到String
	static public String getFileContent(String file){
		return getFileContent(new File(file));
	}

	//保存String到文件
	static public boolean saveContentToFile(byte[]data,File f){
		
		BufferedOutputStream bos;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(f));
			bos.write(data);
			bos.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	static public boolean saveContentToFile(String data,File f){
		return saveContentToFile(data.getBytes(),f);
	}

	static public boolean saveContentToFile(byte[] data,String f){
		return saveContentToFile(data,new File(f));
	}

	static public boolean saveContentToFile(String data,String f){
		return saveContentToFile(data.getBytes(),new File(f));
	}

	static public boolean ObjectToFile(Object object,File f){
		ObjectOutputStream oos = null;
		boolean ret = false;
		if(object == null || !(object instanceof Serializable))return ret;
		try{
			oos = new ObjectOutputStream(new FileOutputStream(f));
			oos.writeObject(object);
			ret = true;
		} catch (FileNotFoundException e){
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(oos!=null){
				try {
					oos.close();
					f.setLastModified(System.currentTimeMillis());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	static public boolean ObjectToFile(Object object,String f){
		return ObjectToFile(object,new File(f));
	}
	
	static public Object ObjectFromFile(File f){
		ObjectInputStream ois = null;
		Object obj = null;
		try{
			ois = new ObjectInputStream(new FileInputStream(f));
			obj = ois.readObject();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}finally{
			if(ois !=null){
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return obj;
	}

	static public Object ObjectFromFile(String f){
		return ObjectFromFile(new File(f));
	}
	
	static public Object ObjectFromFileNullWhenTimeout(File f){
		if(System.currentTimeMillis()-f.lastModified()>OBJECT_CACHE_TIMEOUT){
			return null;
		}
		return ObjectFromFile(f);
	}

	static public void AlertDialogMessage(Context context,String title,String msg,String buttonText,DialogInterface.OnClickListener listener)
	{
		if(listener == null){
			listener = new DialogInterface.OnClickListener(){
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			};
		}
		if(buttonText == null || buttonText.length() <1)buttonText = context.getString(android.R.string.ok);
		new AlertDialog.Builder(context).setTitle(title).setMessage(msg).setNegativeButton(buttonText,listener).setCancelable(false).create().show();
	}

	static public void AlertDialogMessage(Context context,String title,String msg,DialogInterface.OnClickListener listener)
	{
		AlertDialogMessage(context,title,msg,null,listener);
	}

	public static void AlertDialogMessage(Context context, String title,String msg) {
		AlertDialogMessage(context,title,msg,null,null);
	}

	static public  AlertDialog AlertDialogMessageWithoutButton(Context context,String title,String msg)
	{
		return new AlertDialog.Builder(context).setTitle(title).setMessage(msg).setCancelable(false).create();
	}

	static public void AlertDialogConfirm(
			Context context,
			String title,
			String msg,
			String button_positive,
			String button_negative,
			DialogInterface.OnClickListener positiveListener,
			DialogInterface.OnClickListener negativeListener){
	    if(negativeListener == null){
	        negativeListener = new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            };
	    }
		new AlertDialog.Builder(context).setTitle(title).setMessage(msg)
//		.setNeutralButton(button_negative,negativeListener)
		.setNegativeButton(button_negative,negativeListener)
		.setPositiveButton(button_positive, positiveListener)
		.setCancelable(false).create().show();
	}

	static public void AlertDialogConfirm(
			Context context,
			String title,
			String msg,
			DialogInterface.OnClickListener positiveListener,
			DialogInterface.OnClickListener negativeListener){
		AlertDialogConfirm(context,title,msg,
				context.getString(android.R.string.ok),
				context.getString(android.R.string.cancel),
				positiveListener,negativeListener);
	}

	static public String toHexString(byte[] data)
    {
    	String ret = "";
    	for(byte i:data){
    		ret = ret+String.format("%02x", i);
    	}
    	return ret;
    }

    static public String hashChecksum(byte[] data,String algorithm){
		MessageDigest digester;
		String hash = null;
		try {
			digester = MessageDigest.getInstance(algorithm);
			digester.update(data);
			byte[] digest = digester.digest();
			hash = toHexString(digest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hash;
	}

	static public String sha1sum(byte[] data){
		return hashChecksum(data,"sha1");
	}

	static public String md5sum(byte[] data){
		return hashChecksum(data,"md5");
	}

    static public String md5sum(String data){
        return hashChecksum(string2Bytes(data),"md5");
    }

    static public boolean checkInArrary(Object[]vv,Object o){
		if(vv == null)return false;
		if(o == null)return false;
		for(int i=0;i<vv.length;i++){
			if(vv[i].equals(o))return true;
		}
		return false;
	}

	static public long getFileSize(File f) throws Exception {
		long s = 0;

		if(f.isDirectory()){
			s = getDirectorySize(f);
		}else if (f.isFile()){
			s = f.length();
		}
		return s;
	}

	static private long getDirectorySize(File f) throws Exception {
		long size = 0;
		File flist[] = f.listFiles();
		for (int i = 0; i < flist.length; i++){
			if (flist[i].isDirectory()) {
				size = size + getFileSize(flist[i]);
			}else{
				size = size + flist[i].length();
			}
		}
		return size;
	}

	public static long getFileSize(String d) {
		try {
			return getFileSize(new File(d));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	static public long String2Long(String ss){
		if(ss == null)return 0;
		ss = ss.trim();
		String s =  ss.replaceFirst("^[-+]?[0-9]+","");
		String d = ss.replaceFirst(s+"\\Z","");
		long rs = 0;
		try{
			rs = Long.parseLong(d);
		}catch (Exception e){
			rs = 0;
		}
		return rs;
	}

	static public int String2Int(String ss){
		if(ss == null)return 0;
		ss = ss.trim();
		String s =  ss.replaceFirst("^[-+]?[0-9]+","");
		String d = ss.replaceFirst(s+"\\Z","");
		int rs = 0;
		try{
			rs = Integer.parseInt(d);
		}catch (Exception e){
			rs = 0;
		}
		return rs;
	}

	static public double String2Double(String ss){
		if(ss == null)return 0;
		ss = ss.trim();
		String s =  ss.replaceFirst("^[-+]?[0-9]+","");
		String d = ss.replaceFirst(s+"\\Z","");
		double rs = 0;
		try{
			rs = Double.parseDouble(d);
		}catch (Exception e){
			rs = 0;
		}
		return rs;
	}

	public static Bitmap drawableToBitmap(Drawable drawable)
	{
		Bitmap bitmap =  null;
		if(drawable == null)return null;
        if(drawable instanceof BitmapDrawable){
        	bitmap = ((BitmapDrawable)drawable).getBitmap();
        }else{
	        bitmap = Bitmap.createBitmap(
                                        drawable.getIntrinsicWidth(),
                                        drawable.getIntrinsicHeight(),
                                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                        : Bitmap.Config.RGB_565);
	        Canvas canvas = new Canvas(bitmap);
	        //canvas.setBitmap(bitmap);
	        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
	        drawable.draw(canvas);
        }
        return bitmap;
    }

	public static byte[] drawableToBytes(Drawable drawable)
	{
		return Bitmap2Bytes(drawableToBitmap(drawable));
	}

	static public byte[] Bitmap2Bytes(Bitmap bm)
	{
		if(bm == null)return null;
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
	    return baos.toByteArray();
	}

	static public Bitmap Bytes2Bimap(byte[] b)
	{
        if(b.length!=0){
        	return BitmapFactory.decodeByteArray(b, 0, b.length);
        }else{
        	return null;
        }
    }

	static public boolean Bitmap2File(Bitmap bm,File file){
		if(bm == null)return false;
		BufferedOutputStream bos;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
		    bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
		    bos.flush();
		    bos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	    return true;
	}

	static public boolean Drawable2File(Drawable drawable,File file){
		return Bitmap2File(drawableToBitmap(drawable),file);
	}

	static public int compareTime(File file1,File file2)
	{
		long t1 = file1.lastModified();
		long t2 = file2.lastModified();
		if(t1 > t2)return 1;
		else if(t1 == t2 )return 0;
		return -1;
	}

	static public int compareTime(String file1,String file2)
	{
		return compareTime(new File(file1),new File(file2));
	}


	public static HashMap<String,String> getKeyAndValueFromReader(BufferedReader br) throws IOException{
		String line = null;
		String[] vv = null;
		HashMap<String,String> ret = null;
		while((line = br.readLine()) !=null){
			line = line.replaceAll("#.*\\Z", "");
			if(line.indexOf('=') == -1){
				continue;
			}
			vv = line.split("=", 2);
			if(vv!=null){
				if(ret == null)ret = new HashMap<String,String>();
				if(vv.length<2){
					ret.put(vv[0],null);
				}else{
					ret.put(vv[0],vv[1]);
				}
			}
		}
		return ret;
	}
	
	public static HashMap<String,String> getKeyAndValueFromString(String s) throws IOException{
		return getKeyAndValueFromReader(new BufferedReader(new StringReader(s)));
	}

	public static HashMap<String,String> getKeyAndValueFromFile(File file){
		try {
			return getKeyAndValueFromReader(new BufferedReader(new FileReader(file)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			///e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static HashMap<String,String> getKeyAndValueFromFile(String filename) throws IOException{
		return getKeyAndValueFromReader(new BufferedReader(new FileReader(filename)));
	}



	public static final Map<String,String>getMountPoints() throws IOException
	{
		BufferedReader buf = new BufferedReader(new FileReader("/proc/self/mounts"));
		String line = null;
		HashMap<String,String> m = null;
		String[] vv = null;

		while((line = buf.readLine()) != null){
			vv = line.split("[ \t]+",3);
			if(vv != null && vv.length >2){
				if(m == null) m = new HashMap<String,String>();
				m.put(vv[1], vv[0]);
			}
		}
		buf.close();
		return m;
	}

	public static final List<String> getMountPointsInList() throws IOException
	{
		BufferedReader buf = new BufferedReader(new FileReader("/proc/self/mounts"));
		String line = null;
		List<String> m = null;
		String[] vv = null;

		while((line = buf.readLine()) != null){
			vv = line.split("[ \t]+",3);
			if(vv != null && vv.length >2){
				if(m == null) m = new ArrayList<String>();
				m.add(vv[1]);
			}
		}
		buf.close();
		return m;
	}
	
	static public class MiscFilenameFilter implements FilenameFilter{
		private String name = null;
		private String[] suffixs = null;

		public MiscFilenameFilter(String n,String ... sx)
		{
			name = n;
			suffixs = sx;
		}
		
		public MiscFilenameFilter(String n)
		{
			this(n,new String[]{});
		}

		@Override
		public boolean accept(File dir, String filename) {
			boolean ret = true;
			if(name!=null && name.length() >0){
				ret = filename.startsWith(name);
			}
			if(ret && suffixs!=null && suffixs.length >0){
				for(String sf:suffixs){
					ret = filename.toLowerCase().endsWith(sf.toLowerCase());
					if(ret)break;
				}
			}
			return ret;
		}
		
	}

	static public String size2string(long size){
    	String ret = "0B";
    	long B = 1;
    	long kB = B * 1024;
    	long mB = kB * 1024;
    	long gB = mB * 1024;
    	long tB = gB * 1024;
    	if(size<kB){
    		ret = size + "B";
    	}else if(size <mB){
    		ret = String.format("%.1fK", size*1.0/kB);
    	}else if(size <gB){
    		ret = String.format("%.1fM", size*1.0/mB);
    	}else if(size <tB){
    		ret = String.format("%.1fG", size*1.0/gB);
    	}else{
    		ret = String.format("%.1fT", size*1.0/tB);
    	}
    	return ret;
    }

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static List<HashMap<String, String>> diskFilesystem(){
		ArrayList<HashMap<String, String>> ret = null;
		
		try {
			Map<String, String> m = getMountPoints();
			HashMap<String, String>v=null;
			long t=0;
			long f=0;
			long bs=0;
			String dir = null;
			String dev = null;
			
			if(m!=null&& m.size()>0){
				for(Entry<String, String> o:m.entrySet()){
					dir = o.getKey();
					dev = o.getValue();
					v = new HashMap<String, String>();
					v.put("dev", dev);
					v.put("dir", dir);
					if(!dev.startsWith("/"))continue;
					StatFs sf = null;
					
					try{
						sf = new StatFs(dir);
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}

					if(Build.VERSION.SDK_INT >= 18){
						bs=sf.getBlockSizeLong();
						t=sf.getBlockCountLong()*bs;
						f=sf.getAvailableBlocksLong()*bs;
					}else{
						bs=sf.getBlockSize();
						t=sf.getBlockCount()*bs;
						f=sf.getAvailableBlocks()*bs;
					}
					v.put("total", size2string(t));
					v.put("free", size2string(f));
					if(ret==null)ret = new ArrayList<HashMap<String,String>>();
					ret.add(v);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ret;
	}

	///in second
	static public long currentTime(){
		return (long) (System.currentTimeMillis()/1000.0);
	}

	
    static public void OpenPackageFile(Context c,String filePath)
    {
	    Intent intent = new Intent();
	    intent.setAction(android.content.Intent.ACTION_VIEW);
	    intent.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    c.startActivity(intent);
    }

    static private FileChannel getLockFileChannel(String file,String mode) throws FileNotFoundException
    {
    	FileChannel fc = new RandomAccessFile(file, "rw").getChannel();
    	return fc;
    }
    
    static private FileChannel readFileChannel(String file) throws FileNotFoundException
    {
    	FileChannel fc = new RandomAccessFile(file, "r").getChannel();
    	return fc;
    }
    
    static private FileChannel writeFileChannel(String file) throws FileNotFoundException
    {
    	FileChannel fc = new RandomAccessFile(file, "rw").getChannel();
    	return fc;
    }

    static private String lockReadFile(String file)
    {
    	String ret = null;
    	FileLock lk = null;
    	try {
			FileChannel bis = readFileChannel(file);
			lk = bis.lock();
			ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
			int n = 0;
			try {
				
				while ((n = bis.read(buf)) != -1) {
					if (ret == null) {
						ret = new String(buf.array(), 0, n);
					} else {
						ret = ret + new String(buf.array(), 0, n);
					}
					buf.clear();
				}
				bis.close();
			} catch (FileNotFoundException e) {
				// e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return ret;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}finally{
			try{
				if(lk!=null)lk.release();
			}catch(Exception e){}
		}
    	
    	return ret;
    }
    
    static public boolean lockWriteFile(String file,byte []data)
    {
    	FileChannel bos = null;
    	FileLock lk = null;
		try {
			bos = writeFileChannel(file);
			lk = bos.lock();
			bos.write(ByteBuffer.wrap(data));
			bos.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try{
				if(lk!=null)lk.release();
			}catch(Exception e){}
		}
		return false;
    }

    private static BitmapFactory.Options getOptions(BitmapFactory.Options opt,int width,int height)
	{
		int outWidth = opt.outWidth; //获得图片的实际高和宽  
		int outHeight = opt.outHeight;  
		opt.inDither = false;  
		opt.inSampleSize = 1;                            
		        //设置缩放比,1表示原比例，2表示原来的四分之一....  
		        //计算缩放比  
////		Log.i("getOptions","outWidth:"+outWidth+",outHeight:"+outHeight);
		if (outWidth != 0 && outHeight != 0 && width != 0 && height != 0 &&
				width < outWidth && height < outHeight) {  
		    int sampleSize = (outWidth / width + outHeight / height) / 2;  
		    opt.inSampleSize = sampleSize;
		}
		opt.inJustDecodeBounds = false; //最后把标志复原
		//opt.inPreferredConfig = Bitmap.Config.RGB_565;
		return opt;  
	}

	private static BitmapFactory.Options getBitmapOption(String file, int width, int height)
	{
		BitmapFactory.Options opt = new BitmapFactory.Options();  
		opt.inJustDecodeBounds = true;
		opt.inInputShareable=true;  
        opt.inPurgeable=true;//设置图片可以被回收 
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		        //设置只是解码图片的边距，此操作目的是度量图片的实际宽度和高度  
		decodeFileByStream(file, opt);
		
		return getOptions(opt,width,height);
	}

	private static BitmapFactory.Options getBitmapOption(Resources res, int id,int width, int height)
	{
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		opt.inInputShareable=true;  
	    opt.inPurgeable=true;//设置图片可以被回收 
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		        //设置只是解码图片的边距，此操作目的是度量图片的实际宽度和高度  
		decodeResourceByStream(res, id,opt);  
		return getOptions(opt,width,height);
	}

	private static BitmapFactory.Options getBitmapOption(byte bytes[],int width, int height)
	{
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		opt.inInputShareable=true;  
	    opt.inPurgeable=true;//设置图片可以被回收 
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		        //设置只是解码图片的边距，此操作目的是度量图片的实际宽度和高度  
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
		return getOptions(opt,width,height);
	}

	private static BitmapDrawable getBitmapDrawable(Resources res,int resid){
		BitmapFactory.Options opt = null;
		Bitmap bitMap = null;
		opt = getBitmapOption(res,resid,maxImageSize,maxImageSize);
		bitMap = decodeResourceByStream(res,resid,opt);
		return new BitmapDrawable(res, bitMap);
	}

	public  BitmapDrawable getBitmapDrawable(Context context,int resid){
		return getBitmapDrawable(context.getResources(),resid);
	}
	
	public static Bitmap decodeResourceByStream(Resources res,int resid,BitmapFactory.Options opt)
	{
		Bitmap mBitmap = null;
		InputStream fis = null;
		fis = res.openRawResource(resid);
		mBitmap = BitmapFactory.decodeStream(fis,null,opt);
		if(fis !=null){
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fis = null;
		}
		return mBitmap;
	}
	
	public static Bitmap decodeResourceByStream(Resources res,int resid)
	{
		return decodeResourceByStream(res,resid,getBitmapOption(res,resid,0,0));
	}
	
	public static Bitmap decodeFileByStream(String filepath,BitmapFactory.Options opt)
	{
		Bitmap mBitmap = null;
		FileInputStream fis = null;
		if(filepath == null)return null;
		try {
			fis = new FileInputStream(filepath);
			mBitmap = BitmapFactory.decodeStream(fis,new Rect(0,0,0,0),opt);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(fis !=null){
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fis = null;
		}
		return mBitmap;
	}
	
	public static Bitmap decodeFileByStream(String filepath)
	{
		return decodeFileByStream(filepath,getBitmapOption(filepath,0,0));
	}
	
	public static BitmapDrawable getBitmapDrawable(Context context,String filepath){
		BitmapFactory.Options opt = null;
		Bitmap bitMap = null;
		opt = getBitmapOption(filepath,maxImageSize,maxImageSize);
		bitMap = decodeFileByStream(filepath,opt);
		return new BitmapDrawable(context.getResources(), bitMap);
	}
	
	public static Bitmap getBitmap(String filepath){
		BitmapFactory.Options opt = null;
		Bitmap bitMap = null;
		opt = getBitmapOption(filepath,maxImageSize,maxImageSize);
		bitMap = decodeFileByStream(filepath,opt);
		return bitMap;
	}

	public static Bitmap getBitmap(byte bytes[]){
		BitmapFactory.Options opt = null;
		Bitmap bitMap = null;
		opt = getBitmapOption(bytes,maxImageSize,maxImageSize);
		bitMap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);
		return bitMap;
	}

	private static int maxImageSize = 144;


    static public String Size2String(long size){
    	String ret = "0B";
    	long B = 1;
    	long kB = B * 1024;
    	long mB = kB * 1024;
    	long gB = mB * 1024;
    	long tB = gB * 1024;
    	if(size<kB){
    		ret = size + "B";
    	}else if(size <mB){
    		ret = String.format("%.1fK", size*1.0/kB);
    	}else if(size <gB){
    		ret = String.format("%.1fM", size*1.0/mB);
    	}else if(size <tB){
    		ret = String.format("%.1fG", size*1.0/gB);
    	}else{
    		ret = String.format("%.1fT", size*1.0/tB);
    	}
    	return ret;
    }

	public static int dip2px(Context context, float dipValue)
	{
        final float scale = context.getResources().getDisplayMetrics().density; 
        return (int)(dipValue * scale + 0.5f); 
	} 
	
	public static int px2dip(Context context, float pxValue)
    {
        final float scale = context.getResources().getDisplayMetrics().density; 
        return (int)(pxValue / scale + 0.5f);
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	static public boolean hasMenuKey(Context context){
		boolean hasMenuKey = true;
		if(Build.VERSION.SDK_INT >=14){
			hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
		}
		return hasMenuKey;
	}
	
	public static Bitmap convertViewToBitmap(View view){
        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        ///Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));

        return bitmap;
	}

   static public byte[] string2Bytes(String s){
        byte[] ret = null;
        if(s == null)return ret;
        try {
            ret = s.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    static public String bytes2String(byte s[]){
        try {
            return new String(s,"ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    static public void simpleNotification(Context c,Intent it,int id,int icon,int defaults,CharSequence ti,CharSequence message){

    	Builder bd = new NotificationCompat.Builder(c)
    				.setTicker(ti)
    				.setContentTitle(ti)
    				.setContentText(message)
    				.setAutoCancel(true);
    	if(icon!=0)bd.setSmallIcon(icon);
    	PendingIntent pi;
    	if(it!=null){
    		it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
    		pi = PendingIntent.getActivity(c, id, it, PendingIntent.FLAG_UPDATE_CURRENT
					| Intent.FLAG_ACTIVITY_NEW_TASK);
    	}else{
    		it = new Intent(Intent.ACTION_SEND);
    		it.setType("text/plain");
	        it.putExtra(Intent.EXTRA_SUBJECT,ti);
	        it.putExtra(Intent.EXTRA_TEXT, message);
	        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		pi = PendingIntent.getActivity(c, id, Intent.createChooser(it,"选择查看程序"), PendingIntent.FLAG_UPDATE_CURRENT
					| Intent.FLAG_ACTIVITY_NEW_TASK);
    	}
		bd.setContentIntent(pi);
    	bd.setWhen(System.currentTimeMillis());
    	NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    	//int defaults = Notification.DEFAULT_VIBRATE;
    	//defaults |= Notification.DEFAULT_SOUND;
    	bd.setDefaults(defaults);
    	nm.notify(id, bd.build());
    }
    
    static public void removeNotification(Context c,int id)
    {
    	NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    	nm.cancel(id);
    }
    
    static public String unicode2String(String unicodeStr){  
        StringBuffer sb = new StringBuffer();  
        String str[] = unicodeStr.toUpperCase().split("U");  
        for(int i=0;i<str.length;i++){  
          if(str[i].equals("")) continue;  
          char c = (char)Integer.parseInt(str[i].trim(),16);  
          sb.append(c);  
        }  
        return sb.toString();  
      }

    /** 
     * 把中文转成Unicode码 
     * @param str 
     * @return 
     */  
    public static String chinaToUnicode(String str){  
        String result="";  
        for (int i = 0; i < str.length(); i++){  
            int chr1 = (char) str.charAt(i);  
            if(chr1>=19968&&chr1<=171941){//汉字范围 \u4e00-\u9fa5 (中文)  
                result+="\\u" + Integer.toHexString(chr1);  
            }else{  
                result+=str.charAt(i);  
            }  
        }
        return result;  
    }  
  
    /** 
     * 判断是否为中文字符 
     * @param c 
     * @return 
     */  
    public  boolean isChinese(char c) {  
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);  
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS  
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS  
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A  
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION  
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION  
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {  
            return true;  
        }  
        return false;
    }
    
    
    static public final SimpleDateFormat SDF_YY_MM_DD = new SimpleDateFormat("yyyy/MM/dd");
    public static boolean isSameDateTo(Date d,long t){
    	return SDF_YY_MM_DD.format(d).equalsIgnoreCase(SDF_YY_MM_DD.format(new Date(t)));
    }

    public static boolean isSameDateTo(long d,long t){
    	return isSameDateTo(new Date(d),t);
    }

    public static boolean isToday(long t){
    	return isSameDateTo(new Date(),t);
    }
    
    public static String getCommandOutput(String cmd){
    	
    	byte[] buf = new byte[BUFFER_SIZE];
		String ret = null;
		BufferedInputStream bis = null;
		int n = 0;

		try {
			bis = new BufferedInputStream(Runtime.getRuntime().exec(cmd).getInputStream());
			while((n = bis.read(buf)) != -1){
				if(ret == null){
					ret = new String(buf,0,n);
				}else{
					ret = ret + new String(buf,0,n);
				}
			}
			bis.close();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
    }
    
    public static String second2String(long sec)
    {
    	String ret="";
    	if(sec<60)
    	{
    		ret = "1分内";
    	}else if(sec<60*60){
/*    		if(sec%60 !=0){
    			ret = sec%60+"秒";
    		}*/
			ret = ((int)sec/60)+"分"+ret;
    	}else {
/*    		if(sec%60 !=0){
    			ret = sec%60+"秒";
    		}*/
    		int min = (int) (sec/60);
    		if(min %60!=0){
    			ret = ((int)min%60)+"分"+ret;
    		}
    		ret = ((int)(sec/(60*60)))+"小时"+ret;
    	}
    	
    	return ret;
    }
    
    static private String macAddress = null;

	public static String getWifiMacAddress(Context context) {
		if(macAddress!=null && macAddress.length()>0)return macAddress;
		try {
			WifiManager wifi = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			if (wifi == null)
				return macAddress;
			WifiInfo info = wifi.getConnectionInfo();
			macAddress = info.getMacAddress();
			if (macAddress == null && !wifi.isWifiEnabled()) {
				wifi.setWifiEnabled(true);
				for (int i = 0; i < 10; i++) {
					WifiInfo _info = wifi.getConnectionInfo();
					if (_info.getMacAddress() != null) {
						macAddress = _info.getMacAddress();
						break;
					}
				}
				wifi.setWifiEnabled(false);
			}
		} catch (Exception e) {
		}
		return macAddress;
	}
	
	
	static public void shareTextTo(Context context,String subject,String text)
	{
		Intent shareInt = new Intent(Intent.ACTION_SEND);
        shareInt.setType("text/plain");   
        shareInt.putExtra(Intent.EXTRA_SUBJECT,subject);
        shareInt.putExtra(Intent.EXTRA_TEXT,text);
        shareInt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(shareInt,"分享到:"));
	}
	
	static public void shareImageTo(Context context,String filepath,String text)
	{
		Intent shareInt = new Intent(Intent.ACTION_SEND);
        shareInt.setType("image/*");   
        shareInt.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filepath)));
        if(text!=null && text.length()>0)shareInt.putExtra("sms_body", text);
        shareInt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(shareInt,"分享到:"));
	}
}
