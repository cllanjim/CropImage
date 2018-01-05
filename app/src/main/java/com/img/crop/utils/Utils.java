package com.img.crop.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Video;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.Locale;

public class Utils {
    private static final String TAG = "Utils";
    private static final String DEBUG_TAG = "CropDebug";
    public final static int EMAIL_ATTACHMENT_MAX = 0x3200000;
    public final static int SHARE_NUM_MAX = 100;

    private static final long POLY64REV = 0x95AC9329AC4BC9B5L;
    private static final long INITIALCRC = 0xFFFFFFFFFFFFFFFFL;
    public static final int DRAG_ALPHA = (int)(0.85f * 255);

    private static long[] sCrcTable = new long[256];

    private static final boolean IS_DEBUG_BUILD =
            Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");

    private static final String MASK_STRING = "********************************";
    
    public static final String BLUETOOTH_PACKAGE_NAME = "com.android.bluetooth";
    public static final String MMS_PACKAGE_NAME = "com.android.mms";
    public static final String EMAIL_PACKAGE_NAME = "com.android.email";
    
    public static float sFontScale;
    public final static float FONT_SCALE_NORMAL = 1.0f;
    public final static float FONT_SCALE_BIG = 1.2f;
    public static Locale sLocale;

    // Throws AssertionError if the input is false.
    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    // Throws AssertionError with the message. We had a method having the form
    //   assertTrue(boolean cond, String message, Object ... args);
    // However a call to that method will cause memory allocation even if the
    // condition is false (due to autoboxing generated by "Object ... args"),
    // so we don't use that anymore.
    public static void fail(String message, Object ... args) {
        throw new AssertionError(
                args.length == 0 ? message : String.format(message, args));
    }

    // Throws NullPointerException if the input is null.
    public static <T> T checkNotNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }

    // Returns true if two input Object are both null or equal
    // to each other.
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a == null ? false : a.equals(b));
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    public static int prevPowerOf2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    // Returns the input value x clamped to the range [min, max].
    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    // Returns the input value x clamped to the range [min, max].
    public static float clamp(float x, float min, float max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    // Returns the input value x clamped to the range [min, max].
    public static long clamp(long x, long min, long max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public static boolean isOpaque(int color) {
        return color >>> 24 == 0xFF;
    }

    public static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * A function thats returns a 64-bit crc for string
     *
     * @param in input string
     * @return a 64-bit crc value
     */
    public static final long crc64Long(String in) {
        if (in == null || in.length() == 0) {
            return 0;
        }
        return crc64Long(getBytes(in));
    }

    static {
        // http://bioinf.cs.ucl.ac.uk/downloads/crc64/crc64.c
        long part;
        for (int i = 0; i < 256; i++) {
            part = i;
            for (int j = 0; j < 8; j++) {
                long x = ((int) part & 1) != 0 ? POLY64REV : 0;
                part = (part >> 1) ^ x;
            }
            sCrcTable[i] = part;
        }
    }

    public static final long crc64Long(byte[] buffer) {
        long crc = INITIALCRC;
        for (int k = 0, n = buffer.length; k < n; ++k) {
            crc = sCrcTable[(((int) crc) ^ buffer[k]) & 0xff] ^ (crc >> 8);
        }
        return crc;
    }

    public static byte[] getBytes(String in) {
        byte[] result = new byte[in.length() * 2];
        int output = 0;
        for (char ch : in.toCharArray()) {
            result[output++] = (byte) (ch & 0xFF);
            result[output++] = (byte) (ch >> 8);
        }
        return result;
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            Log.w(TAG, "close fail", t);
        }
    }

    public static int compare(long a, long b) {
        return a < b ? -1 : a == b ? 0 : 1;
    }

    public static int ceilLog2(float value) {
        int i;
        for (i = 0; i < 31; i++) {
            if ((1 << i) >= value) break;
        }
        return i;
    }

    public static int floorLog2(float value) {
        int i;
        for (i = 0; i < 31; i++) {
            if ((1 << i) > value) break;
        }
        return i - 1;
    }

    public static void closeSilently(ParcelFileDescriptor fd) {
        try {
            if (fd != null) fd.close();
        } catch (Throwable t) {
            Log.w(TAG, "fail to close", t);
        }
    }

    public static void closeSilently(Cursor cursor) {
        try {
            if (cursor != null) cursor.close();
        } catch (Throwable t) {
            Log.w(TAG, "fail to close", t);
        }
    }

    public static float interpolateAngle(
            float source, float target, float progress) {
        // interpolate the angle from source to target
        // We make the difference in the range of [-179, 180], this is the
        // shortest path to change source to target.
        float diff = target - source;
        if (diff < 0) diff += 360f;
        if (diff > 180) diff -= 360f;

        float result = source + diff * progress;
        return result < 0 ? result + 360f : result;
    }

    public static float interpolateScale(
            float source, float target, float progress) {
        return source + progress * (target - source);
    }

    public static String ensureNotNull(String value) {
        return value == null ? "" : value;
    }

    public static float parseFloatSafely(String content, float defaultValue) {
        if (content == null) return defaultValue;
        try {
            return Float.parseFloat(content);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseIntSafely(String content, int defaultValue) {
        if (content == null) return defaultValue;
        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isNullOrEmpty(String exifMake) {
        return TextUtils.isEmpty(exifMake);
    }

    public static void waitWithoutInterrupt(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            Log.w(TAG, "unexpected interrupt: " + object);
        }
    }

    public static boolean handleInterrruptedException(Throwable e) {
        // A helper to deal with the interrupt exception
        // If an interrupt detected, we will setup the bit again.
        if (e instanceof InterruptedIOException
                || e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }
        return false;
    }

    /**
     * @return String with special XML characters escaped.
     */
    public static String escapeXml(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = s.length(); i < len; ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                case '\"': sb.append("&quot;"); break;
                case '\'': sb.append("&#039;"); break;
                case '&':  sb.append("&amp;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String getUserAgent(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException("getPackageInfo failed");
        }
        return String.format("%s/%s; %s/%s/%s/%s; %s/%s/%s",
                packageInfo.packageName,
                packageInfo.versionName,
                Build.BRAND,
                Build.DEVICE,
                Build.MODEL,
                Build.ID,
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                Build.VERSION.INCREMENTAL);
    }

    public static String[] copyOf(String[] source, int newSize) {
        String[] result = new String[newSize];
        newSize = Math.min(source.length, newSize);
        System.arraycopy(source, 0, result, 0, newSize);
        return result;
    }

    // Mask information for debugging only. It returns <code>info.toString()</code> directly
    // for debugging build (i.e., 'eng' and 'userdebug') and returns a mask ("****")
    // in release build to protect the information (e.g. for privacy issue).
    public static String maskDebugInfo(Object info) {
        if (info == null) return null;
        String s = info.toString();
        int length = Math.min(s.length(), MASK_STRING.length());
        return IS_DEBUG_BUILD ? s : MASK_STRING.substring(0, length);
    }

    // This method should be ONLY used for debugging.
    public static void debug(String message, Object ... args) {
        Log.v(DEBUG_TAG, String.format(message, args));
    }
    
    public static void fitRectFInto(RectF src, RectF dst, RectF out){
    	if (src == null || dst == null || out == null)
    		return;
    	
    	float srcWidth = src.width();
    	float srcHeight = src.height();
    	float dstWidth = dst.width();
    	float dstHeight = dst.height();
    	
    	float retWidth = 0;
    	float retHeight = 0;
    	if (srcWidth > 0 && srcHeight > 0 && dstWidth > 0 && dstHeight > 0){
    		
			if (srcWidth * dstHeight < srcHeight * dstWidth) {
				retWidth = srcWidth * dstHeight / srcHeight;
				retHeight = dstHeight;
			} else {
				retHeight = srcHeight * dstWidth / srcWidth;
				retWidth = dstWidth;
			}
    		
			out.set(0,0,retWidth, retHeight);
    	}
    }
    
    public static String truncateString(String str, float textSize,float freeSpace,TextUtils.TruncateAt where) {
    	String resultStr = (str != null ? str : "");
    	TextPaint paint = new TextPaint();
    	paint.setTextSize(textSize);
    	paint.setAntiAlias(true);
    	if (textSize <=0  || freeSpace <= 0) {
    		return resultStr;
    	}
    	resultStr = TextUtils.ellipsize(resultStr, paint, freeSpace, where).toString();
    	return resultStr;
    }
    
    public static void fitRectInto(Rect src, Rect dst, Rect out){
    	if (src == null || dst == null || out == null)
    		return;
    	
    	float srcWidth = src.width();
    	float srcHeight = src.height();
    	float dstWidth = dst.width();
    	float dstHeight = dst.height();
    	
    	float retWidth = 0;
    	float retHeight = 0;
    	if (srcWidth > 0 && srcHeight > 0 && dstWidth > 0 && dstHeight > 0){
    		
			if (srcWidth * dstHeight < srcHeight * dstWidth) {
				retWidth = srcWidth * dstHeight / srcHeight;
				retHeight = dstHeight;
			} else {
				retHeight = srcHeight * dstWidth / srcWidth;
				retWidth = dstWidth;
			}
    		
			out.set(0,0,(int)retWidth, (int)retHeight);
    	}
    }
    
	public static String getFormat(String filePath) {
		int dot = filePath.lastIndexOf('.');
		int pathLength = filePath.length();
		if (dot != -1 && dot != pathLength - 1) {
			return filePath.substring(dot + 1, pathLength);
		}
		return null;
	}

	public static String getTime(long seconds) {
		long duration = seconds;
		String durationString;

		long hour, minute, second;
		hour = duration / 3600;
		duration %= 3600;
		minute = duration / 60;
		duration %= 60;
		second = duration;

		if (hour != 0) {
			durationString = String.format("%d:%02d:%02d", hour, minute, second);
		} else {
			durationString = String.format("%02d:%02d", minute, second);
		}

		return durationString;
	}
	
	public static boolean isFileNameLegal(String fileName){
    	if (fileName == null){
    		return false;
    	}
    	// The Chinese corresponding chars is legal
    	return fileName.matches("[^|\\\\/:?*\"<>]*");
    }

    private static DecimalFormat mFormater = new DecimalFormat("#.##");
    /**
     * @param length 文件长度
     * @return 带有合适单位名称的文件大小
     */
    public static String getSizeFormatText(long length) {
        if (length <= 0)
            return "0KB";

        String str = "B";
        double result = (double) length;
        if(length < 1024){
            return "1KB";
        }
        // 以1024为界，找到合适的文件大小单位
        if (result >= 1024) {
            str = "KB";
            result /= 1024;
            if (result >= 1024) {
                str = "MB";
                result /= 1024;
            }
            if (result >= 1024) {
                str = "GB";
                result /= 1024;
            }
        }
        String sizeString = null;

        // 按照需求设定文件的精度
        // MB 和 GB 保留两位小数
        if (str.equals("MB") || str.equals("GB")) {
            sizeString = mFormater.format(result);
        }
        // B 和 KB 保留到各位
        else
            sizeString = Integer.toString((int) result);
        return sizeString + str;
    }

    public static final int TARGET_SIZE_MINI_THUMBNAIL = 320;
    public static final int TARGET_SIZE_MICRO_THUMBNAIL = 96;

    public static Bitmap createThumbFromVideo(File video) {
        if (video.exists() == false) {
            return null;
        }
        try {
            Bitmap pic = ThumbnailUtils.createVideoThumbnail(video.toString(),Video.Thumbnails.MICRO_KIND);
            if (pic != null) {
                return pic;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * The minimum quality parameter which is used to compress JPEG images.
     */
    private static final int MINIMUM_IMAGE_COMPRESSION_QUALITY = 50;
    /**
     * The quality parameter which is used to compress JPEG images.
     */
    private static final int IMAGE_COMPRESSION_QUALITY = 95;
    private static final int NUMBER_OF_RESIZE_ATTEMPTS = 8;
    private synchronized static byte[] getResizedImageData(int width, int height,
            int widthLimit, int heightLimit, int byteLimit, Uri uri, Context context, int degree) {
        final boolean DEBUG = false;
        int outWidth = width;
        int outHeight = height;

        InputStream input = null;
        try {
            ByteArrayOutputStream os = null;
            int attempts = 1;
            int sampleSize = computeSample(outWidth, outHeight, widthLimit, heightLimit);
            BitmapFactory.Options options = new BitmapFactory.Options();
            int quality = IMAGE_COMPRESSION_QUALITY;
            Bitmap b = null;

            // In this loop, attempt to decode the stream with the best possible subsampling without running
            // out of memory.
            long time = System.currentTimeMillis();
            do {
                options.inSampleSize = sampleSize;
                try {
                    if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                        try {
                            String filePath = URLDecoder.decode(uri.toString().substring("file://".length(), uri.toString().length()), "utf-8");
                            b = BitmapFactory.decodeFile(filePath, options);
                        }catch (Exception e) {
                        }
                    } else {
                        input = context.getContentResolver().openInputStream(uri);
                        b = BitmapFactory.decodeStream(input, null, options);
                    }

                    // Couldn't decode and it wasn't because of an exception, bail.
                    if (b == null) return null;
                } catch (OutOfMemoryError e) {
                    if (DEBUG) Log.w(TAG, "getResizedImageData: img too large to decode (OutOfMemoryError), " +
                            "may try with larger sampleSize. Curr sampleSize=" + sampleSize);
                    sampleSize *= 2;// works best as a power of two
                    attempts++;
                    continue;
                } finally {
                    Utils.closeSilently(input);
                }
            } while (b == null && attempts < NUMBER_OF_RESIZE_ATTEMPTS);
            if (DEBUG) Log.w(TAG, "getResizedImageData, decode stream loop pay time:"+(System.currentTimeMillis() - time));
            if (b == null) {
                Log.e(TAG, "getResizedImageData, gave up after too many attempts to resize");
                return null;
            }

            int size = Math.min(b.getWidth(), b.getHeight());
            int x = (b.getWidth() - size)/2;
            int y = (b.getHeight() - size)/2;
            Bitmap squareBitmap = Bitmap.createBitmap(b, x, y, size, size);

            outWidth = Math.min(options.outWidth, options.outHeight);
            outHeight = Math.min(options.outWidth, options.outHeight);

            float scaleFactor = 1.F;
            while ((outWidth * scaleFactor > widthLimit) || (outHeight * scaleFactor > heightLimit)) {
                scaleFactor *= .75F;
            }

            boolean resultTooBig = true;
            // reset count for second loop
            attempts = 1;
            // In this loop, we attempt to compress/resize the content to fit the given dimension
            // and file-size limits.
            time = System.currentTimeMillis();
            do {
                try {
                    if (options.outWidth > widthLimit || options.outHeight > heightLimit ||
                            (os != null && os.size() > byteLimit)) {
                        // The decoder does not support the inSampleSize option.
                        // Scale the bitmap using Bitmap library.
                        int scaledWidth = (int)(outWidth * scaleFactor);
                        int scaledHeight = (int)(outHeight * scaleFactor);

                        if (DEBUG) Log.i(TAG, "getResizedImageData: retry scaling using " +
                                "Bitmap.createScaledBitmap: w=" + scaledWidth +
                                ", h=" + scaledHeight);

                        squareBitmap = Bitmap.createScaledBitmap(squareBitmap, scaledWidth, scaledHeight, false);
                        if (squareBitmap == null) {
                            Log.e(TAG, "getResizedImageData: Bitmap.createScaledBitmap returned NULL!");
                            return null;
                        }
                    }

                    // Compress the image into a JPG. Start with MessageUtils.IMAGE_COMPRESSION_QUALITY.
                    // In case that the image byte size is still too large reduce the quality in
                    // proportion to the desired byte size.
                    os = new ByteArrayOutputStream();
                    squareBitmap.compress(CompressFormat.JPEG, quality, os);
                    int jpgFileSize = os.size();
                    if (jpgFileSize > byteLimit) {
                        quality = (quality * byteLimit) / jpgFileSize;  // watch for int division!
                        if (quality < MINIMUM_IMAGE_COMPRESSION_QUALITY) {
                            quality = MINIMUM_IMAGE_COMPRESSION_QUALITY;
                        }
                        if (DEBUG) Log.i(TAG, "getResizedImageData: compress(2) w/ quality=" + quality);

                        os = new ByteArrayOutputStream();
                        squareBitmap.compress(CompressFormat.JPEG, quality, os);
                    }
                } catch (OutOfMemoryError e) {
                    if (DEBUG) Log.w(TAG, "getResizedImageData, image too big (OutOfMemoryError), will try "
                            + " with smaller scale factor, cur scale factor: " + scaleFactor);
                }
                scaleFactor *= .75F;
                attempts++;
                resultTooBig = os == null || os.size() > byteLimit;
            } while (resultTooBig && attempts < NUMBER_OF_RESIZE_ATTEMPTS);
            if (DEBUG) Log.w(TAG, "getResizedImageData,limit loop pay time:"+(System.currentTimeMillis() - time));
            squareBitmap.recycle();
            squareBitmap = null;
            b.recycle();
            b = null;
            return resultTooBig ? null : os.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "getResizedImageData, FileNotFoundException");
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "getResizedImageData, OutOfMemoryError");
            return null;
        }
    }

    private static int computeSample(int width, int height, int dstWidth, int dstHeight) {
        if (width <= dstWidth && height <= dstHeight) {
            return 1;
        }
        int sample = 1;
        while (width > dstWidth || height > dstHeight){
            sample *= 2;
            width /= sample;
            height /= sample;
        }
        return sample <=1 ? 1: sample;
    }

    public static Bitmap rotateImageView(int angle, Bitmap bitmap, int limWidth, int limHeight) {
        Bitmap dst = Bitmap.createBitmap(limWidth, limHeight, bitmap.getConfig());
        Canvas canvas = new Canvas(dst);
        canvas.rotate(angle, limWidth/2, limHeight/2);
        canvas.drawBitmap(bitmap, null, new Rect(0, 0, limWidth, limHeight), null);
        return dst;
    }

}
