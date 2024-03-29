/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baoxue.wallpaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

public class WallpaperChooserDialogFragment extends DialogFragment implements
		AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

	private static final String TAG = "Launcher.WallpaperChooserDialogFragment";
	private static final String EMBEDDED_KEY = "com.cyanogenmod.trebuchet."
			+ "WallpaperChooserDialogFragment.EMBEDDED_KEY";

	private boolean mEmbedded;
	private Bitmap mBitmap = null;

	private ArrayList<String> mImagesPath;

	private WallpaperLoader mLoader;
	private WallpaperDrawable mWallpaperDrawable = new WallpaperDrawable();

	public static WallpaperChooserDialogFragment newInstance() {
		WallpaperChooserDialogFragment fragment = new WallpaperChooserDialogFragment();
		fragment.setCancelable(true);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(EMBEDDED_KEY)) {
			mEmbedded = savedInstanceState.getBoolean(EMBEDDED_KEY);
		} else {
			mEmbedded = isInLayout();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(EMBEDDED_KEY, mEmbedded);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mLoader != null
				&& mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
			mLoader.cancel(true);
			mLoader = null;
		}
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		/*
		 * On orientation changes, the dialog is effectively "dismissed" so this
		 * is called when the activity is no longer associated with this dying
		 * dialog fragment. We should just safely ignore this case by checking
		 * if getActivity() returns null
		 */
		Activity activity = getActivity();
		if (activity != null) {
			activity.finish();
		}
	}

	/*
	 * This will only be called when in XLarge mode, since this Fragment is
	 * invoked like a dialog in that mode
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		findWallpapers();

		// TODO: The following code is not exercised right now and may be
		// removed
		// if the dialog version is not needed.
		/*
		 * final View v = getActivity().getLayoutInflater().inflate(
		 * R.layout.wallpaper_chooser, null, false);
		 * 
		 * GridView gridView = (GridView) v.findViewById(R.id.gallery);
		 * gridView.setOnItemClickListener(this); gridView.setAdapter(new
		 * ImageAdapter(getActivity()));
		 * 
		 * final int viewInset =
		 * getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_inset
		 * );
		 * 
		 * FrameLayout wallPaperList = (FrameLayout)
		 * v.findViewById(R.id.wallpaper_list); AlertDialog.Builder builder =
		 * new AlertDialog.Builder(getActivity());
		 * builder.setNegativeButton(R.string.wallpaper_cancel, null);
		 * builder.setTitle(R.string.wallpaper_dialog_title);
		 * builder.setView(wallPaperList, viewInset, viewInset, viewInset,
		 * viewInset); return builder.create();
		 */
		return null;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		findWallpapers();

		/*
		 * If this fragment is embedded in the layout of this activity, then we
		 * should generate a view to display. Otherwise, a dialog will be
		 * created in onCreateDialog()
		 */
		if (mEmbedded) {
			View view = inflater.inflate(R.layout.wallpaper_chooser, container,
					false);
			view.setBackgroundDrawable(mWallpaperDrawable);

			final Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
			gallery.setCallbackDuringFling(false);
			gallery.setOnItemSelectedListener(this);
			gallery.setAdapter(new ImageAdapter(getActivity()));

			View setButton = view.findViewById(R.id.set);
			setButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					selectWallpaper(gallery.getSelectedItemPosition());
				}
			});
			return view;
		}
		return null;
	}

	private void selectWallpaper(int position) {
		try {
			WallpaperManager wpm = (WallpaperManager) getActivity()
					.getSystemService(Context.WALLPAPER_SERVICE);
			// wpm.setResource(mImages.get(position));
			wpm.setStream(new FileInputStream(mImagesPath.get(position)));
			Activity activity = getActivity();
			activity.setResult(Activity.RESULT_OK);
			activity.finish();
		} catch (IOException e) {
			Log.e(TAG, "Failed to set wallpaper: " + e);
		}
	}

	// Click handler for the Dialog's GridView
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		selectWallpaper(position);
	}

	// Selection handler for the embedded Gallery view
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (mLoader != null
				&& mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
			mLoader.cancel();
		}
		mLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	private void findWallpapers() {
		mImagesPath = new ArrayList<String>();

		File walldir = new File("/system/wall");
		if (walldir.exists()) {
			File[] wallpath = walldir.listFiles();
			for (int i = 0; i < wallpath.length; i++) {
				mImagesPath.add(wallpath[i].getPath());
			}
			Collections.sort(mImagesPath);
		}

	}

	private Bitmap getImage(int index) {
		try {
			String path = mImagesPath.get(index);
			Bitmap image = BitmapFactory.decodeFile(path);
			return image;
		} catch (OutOfMemoryError e) {
			return null;
		}
	}

	private Bitmap getThumbImage(int index) {
		try {
			String path = mImagesPath.get(index);
			Bitmap image = BitmapFactory.decodeFile(path);
			return createThumb(image);
		} catch (OutOfMemoryError e) {
			return null;
		}
	}

	private Bitmap createThumb(Bitmap map) {
		int width = map.getWidth();
		int height = map.getHeight();
		int destWidth = 215;
		float sWidth = (float) destWidth / (float) width;
		float sHeight = (float) destWidth / (float) height;
		float s = Math.min(sWidth, sHeight);
		Matrix matrix = new Matrix();
		matrix.postScale(s, s);
		Bitmap thumbBitmap = Bitmap.createBitmap(map, 0, 0, width, height,
				matrix, true);
		return thumbBitmap;
	}

	private class ImageAdapter extends BaseAdapter implements ListAdapter,
			SpinnerAdapter {
		private LayoutInflater mLayoutInflater;

		ImageAdapter(Activity activity) {
			mLayoutInflater = activity.getLayoutInflater();
		}

		public int getCount() {
			return mImagesPath.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = mLayoutInflater.inflate(R.layout.wallpaper_item, parent,
						false);
			} else {
				view = convertView;
			}

			ImageView image = (ImageView) view
					.findViewById(R.id.wallpaper_image);

			Bitmap map = getThumbImage(position);
			image.setImageBitmap(map);
			Drawable thumbDrawable = image.getDrawable();
			if (thumbDrawable != null) {
				thumbDrawable.setDither(true);
			} else {

			}

			return view;
		}
	}

	class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
		BitmapFactory.Options mOptions;

		WallpaperLoader() {
			mOptions = new BitmapFactory.Options();
			mOptions.inDither = false;
			mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			if (isCancelled())
				return null;
			try {
				return getImage(params[0]);
			} catch (OutOfMemoryError e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap b) {
			if (b == null)
				return;

			if (!isCancelled() && !mOptions.mCancel) {
				// Help the GC
				// if (mBitmap != null) {
				// mBitmap.recycle();
				// }

				View v = getView();
				if (v != null) {
					mBitmap = b;
					mWallpaperDrawable.setBitmap(b);
					v.postInvalidate();
				} else {
					mBitmap = null;
					mWallpaperDrawable.setBitmap(null);
				}
				mLoader = null;
			}
			// else {
			// b.recycle();
			// }
		}

		void cancel() {
			mOptions.requestCancelDecode();
			super.cancel(true);
		}
	}

	/**
	 * Custom drawable that centers the bitmap fed to it.
	 */
	static class WallpaperDrawable extends Drawable {

		Bitmap mBitmap;
		int mIntrinsicWidth;
		int mIntrinsicHeight;

		/* package */void setBitmap(Bitmap bitmap) {
			mBitmap = bitmap;
			if (mBitmap == null)
				return;
			mIntrinsicWidth = mBitmap.getWidth();
			mIntrinsicHeight = mBitmap.getHeight();
		}

		@Override
		public void draw(Canvas canvas) {
			if (mBitmap == null)
				return;
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			int x = (width - mIntrinsicWidth) / 2;
			int y = (height - mIntrinsicHeight) / 2;
			canvas.drawBitmap(mBitmap, x, y, null);
		}

		@Override
		public int getOpacity() {
			return android.graphics.PixelFormat.OPAQUE;
		}

		@Override
		public void setAlpha(int alpha) {
			// Ignore
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			// Ignore
		}
	}
}
