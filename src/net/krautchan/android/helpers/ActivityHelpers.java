package net.krautchan.android.helpers;

/*
* Copyright (C) 2011 Johannes Jander (johannes@jandermail.de)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.Assert;
import net.krautchan.R;
import net.krautchan.android.Defaults;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.activity.KCPostActivity;
import net.krautchan.android.activity.KCThreadViewActivity;
import net.krautchan.data.KCThread;
import net.krautchan.data.PostActivityParams;
import net.krautchan.parser.KCPageParser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;

public class ActivityHelpers {
	static final String TAG = "ActivityHelpers";

	public static void switchToThread(KCThread thread, Activity context) {
		Bundle b = new Bundle();
		Assert.assertNotNull("Assertion threadBbId != null failed in ActivityHelpers::switchToThread() ", thread.dbId);
		b.putLong("threadId", thread.dbId);
		int progInc = (thread.numPostings == 0) ? 5 : (100/thread.numPostings);
		if (progInc == 0) {
			progInc = 1;
		}
		b.putInt("progressIncrement", progInc);
		b.putBoolean("visitedPostsCollapsible", true);

		Intent intent = new Intent(context, KCThreadViewActivity.class);
		intent.putExtras(b);
		
		Thread t = new Thread(new KCPageParser(thread)
		.setBasePath("http://krautchan.net/")
		.setThreadHandler(
				Eisenheinrich.getInstance().getThreadListener())
		.setPostingHandler(
				Eisenheinrich.getInstance().getPostListener()));
		t.start();

		context.startActivity(intent);
	}

	public static void createThreadMask(KCThread curThread, long boardDbId, String contentPreset, Activity context) {
		PostActivityParams params = new PostActivityParams();
		params.curBoardDbId = boardDbId;
		if (null != curThread) {
			params.curThreadDbId = curThread.dbId;
			params.curThreadKCNum = curThread.kcNummer;
		}
		ByteArrayOutputStream boss = new ByteArrayOutputStream();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(boss);
			out.writeObject(params);
			Bundle b = new Bundle();
			b.putByteArray("threadparams", boss.toByteArray());
			b.putString("contentpreset", contentPreset);
			Intent intent = new Intent(context, KCPostActivity.class);
			intent.putExtras(b);
			out.close();
			boss.close();
			context.startActivityForResult(intent, 0);
		} catch (IOException e) {
			Log.e(TAG, "createThreadMask failed", e);
		}
	}
	
	public static Drawable generateViewHeanderBackground(final int viewHeight) {
		ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
		    @Override
		    public Shader resize(int width, int height) {
		    	Resources res = Eisenheinrich.getInstance().getResources();
		        LinearGradient lg = new LinearGradient(0, 0, 0, viewHeight,
		            new int[] { 
		        		res.getColor(R.color.Greengradient3a), 
		        		res.getColor(R.color.Greengradient3b), 
		        		res.getColor(R.color.Greengradient3c), 
		        		res.getColor(R.color.Greengradient3d) },
		            new float[] {
		                0, 0.45f, 0.55f, 1 },
		            Shader.TileMode.REPEAT);
		         return lg;
		    }
		};
		PaintDrawable p = new PaintDrawable();
		p.setShape(new RectShape());
		p.setShaderFactory(sf);
		return p;
	}

	public static ImageButton[] addImageButtonRow(int numColumns,
			TableLayout table, View.OnClickListener listener, Context parent) {
		ImageButton[] buttons = new ImageButton[numColumns];
		TableRow row = new TableRow(parent);
		row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		for (int j = 0; j < numColumns; j++) {
			ImageButton bt = new ImageButton(parent);
			buttons[j] = bt;
			bt.setImageDrawable(parent.getResources().getDrawable(R.drawable.icon));
			bt.setBackgroundDrawable(parent.getResources().getDrawable(R.drawable.button));
			bt.setOnClickListener(listener);
			row.addView(bt);
		}
		table.addView(row);
		return buttons;
	}
	
	public static String downloadToFile (Uri uri) {
		String filePath = null;
		try {
			final String url = Defaults.FILE_PATH + uri.getPath();
			HttpClient client = Eisenheinrich.getInstance().getHttpClient();
			HttpGet request = new HttpGet(url); 
			HttpResponse response = client.execute(request);
			Log.v(TAG, "GET response: " + response.getStatusLine());
			HttpEntity responseEntity = response.getEntity();
			InputStream st = responseEntity.getContent();
			filePath = Defaults.IMAGE_TEMP_DIR+"/"+uri.getPath().replace("/", "");
			//filePath = uri.getPath().replace("/", "");
			FileHelpers.writeToSDFile(filePath, st) ;
			st.close(); 
			return FileHelpers.getSDFile (filePath).getAbsolutePath();
		} catch (Exception e) {
			Log.v(TAG, "EXCEPTION: " + e.getMessage());
		}
		return null;
	}

	public static Bitmap loadBitmap(URL url) {
		Bitmap bm = null;
		InputStream is = null;
		BufferedInputStream bis = null;
		try {
			URLConnection conn = url.openConnection();
			conn.connect();
			is = conn.getInputStream();
			bis = new BufferedInputStream(is, 8192);
			bm = BitmapFactory.decodeStream(bis);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bm;
	}
	
	
	public static Bitmap scaleDownBitmap (Bitmap realImage, float maxImageSize, boolean filter) {
	    float ratio = Math.min(
	            (float) maxImageSize / realImage.getWidth(),
	            (float) maxImageSize / realImage.getHeight());
	    int width = Math.round((float) ratio * realImage.getWidth());
	    int height = Math.round((float) ratio * realImage.getHeight());

	    Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
	            height, filter);
	    return newBitmap;
	}

}
