package io.fotoapparat.hardware.v2.orientation;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.view.TextureView;

import java.util.concurrent.CountDownLatch;

import io.fotoapparat.view.TextureListener;

/**
 * Manages the {@link SurfaceTexture} of a {@link TextureView}.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TextureManager implements TextureListener.Listener, OrientationManager.Listener {

	private final CountDownLatch surfaceLatch = new CountDownLatch(1);
	private final OrientationManager orientationManager;
	private SurfaceTexture surface;
	private TextureView textureView;

	public TextureManager(OrientationManager orientationManager) {
		this.orientationManager = orientationManager;
		orientationManager.setListener(this);
	}

	private static float[] getDst(int orientation, int width, int height) {
		if (orientation == 90) {
			return new float[]{
					0.f, height, // top left
					0.f, 0.f, // top right
					width, height, // bottom left
					width, 0.f, // bottom right
			};
		}
		return new float[]{
				width, 0.f, // top left
				width, height, // top right
				0.f, 0.f, // bottom left
				0.f, height, // bottom right
		};
	}

	/**
	 * Notifies that the display orientation has changed.
	 */
	@Override
	public void onDisplayOrientationChanged() {
		correctOrientation(textureView.getWidth(), textureView.getHeight());
	}

	/**
	 * Set a {@link TextureView} to the manager to interact with
	 *
	 * @param textureView the textureView to interact
	 */
	public void setTextureView(TextureView textureView) {
		this.textureView = textureView;
		surface = textureView.getSurfaceTexture();
		if (surface != null) {
			correctOrientation(textureView.getWidth(), textureView.getHeight());
			surfaceLatch.countDown();
		}
		textureView.setSurfaceTextureListener(new TextureListener(this));
	}

	/**
	 * @return the {@link SurfaceTexture} of the {@link TextureView} but only when it becomes
	 * available.
	 */
	public SurfaceTexture getSurface() {
		try {
			surfaceLatch.await();
		} catch (InterruptedException e) {
			// Do nothing
		}
		return surface;
	}

	@Override
	public void onSurfaceAvailable(SurfaceTexture surface) {
		this.surface = surface;
		surfaceLatch.countDown();
	}

	@Override
	public void onTextureSizeChanged(int width, int height) {
		correctOrientation(width, height);
	}

	private void correctOrientation(int width, int height) {
		final Matrix matrix = new Matrix();
		int screenOrientation = orientationManager.getScreenOrientation();

		if (screenOrientation % 180 == 90) {
			float[] src = {
					0.f, 0.f, // top left
					width, 0.f, // top right
					0.f, height, // bottom left
					width, height, // bottom right
			};

			float[] dst = getDst(screenOrientation, width, height);

			matrix.setPolyToPoly(src, 0, dst, 0, 4);
		}
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				textureView.setTransform(matrix);
			}
		});
	}
}