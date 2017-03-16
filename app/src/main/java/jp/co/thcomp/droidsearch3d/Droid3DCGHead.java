package jp.co.thcomp.droidsearch3d;

import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

import jp.co.thcomp.glsurfaceview.GLCircle;
import jp.co.thcomp.glsurfaceview.GLCylinder;
import jp.co.thcomp.glsurfaceview.GLDrawView;
import jp.co.thcomp.glsurfaceview.GLHemiSphere;
import jp.co.thcomp.glsurfaceview.RotateInfo;
import jp.co.thcomp.util.DisplayUtil;

public class Droid3DCGHead extends Droid3DCGParts {
	private GLHemiSphere mLHornHemiSphere;
	private GLCylinder mLHornCylinder;
	private GLHemiSphere mRHornHemiSphere;
	private GLCylinder mRHornCylinder;
	private GLHemiSphere mHeadHemiSphere;
	private GLCircle mLEyeCircle;
	private GLCircle mREyeCircle;
	private RotateInfo mSwingInfo;
	private RotateInfo mPartsRotateInfo;
	private boolean mFirstSurfaceCreate = true;

	public Droid3DCGHead(GLDrawView view) {
		super(view);

		new Thread(new Runnable(){
			@Override
			public void run() {
				GLDrawView view = mView;

				mLHornHemiSphere = new GLHemiSphere(view);
				mLHornCylinder = new GLCylinder(view);
				mRHornHemiSphere = new GLHemiSphere(view);
				mRHornCylinder = new GLCylinder(view);
				mHeadHemiSphere = new GLHemiSphere(view);
				mLEyeCircle = new GLCircle(view);
				mREyeCircle = new GLCircle(view);

				mLHornHemiSphere.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mLHornCylinder.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mRHornHemiSphere.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mRHornCylinder.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mHeadHemiSphere.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mLEyeCircle.setColors(RobotEyeColorR, RobotEyeColorG, RobotEyeColorB, RobotEyeColorA);
				mREyeCircle.setColors(RobotEyeColorR, RobotEyeColorG, RobotEyeColorB, RobotEyeColorA);
			}
		}).start();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;

		if(mFirstSurfaceCreate){
			float density = DisplayUtil.getDefaultDisplayDensity(mContext);
			Context context = mContext;
			float halfWidth = (((float)width) - Droid3DCGParts.getRobotWidth(context)) / 2;
			float halfHeight = (((float)height) - Droid3DCGParts.getRobotHeight(context)) / 2;
	
			mHeadHemiSphere.setSphereInfo(density * (RobotHeadOffsetX + RobotHeadRadius) + halfWidth, density * (RobotHeadOffsetY + RobotHeadRadius) + halfHeight, DefaultZOrder, density * RobotHeadRadius);
			mLHornHemiSphere.setSphereInfo(density * (RobotLHornOffsetX + RobotHornRadius) + halfWidth, density * (RobotHornOffsetY + RobotHornRadius) + halfHeight, DefaultZOrder, density * RobotHornRadius);
			mLHornHemiSphere.addRotation(RobotHornRotateDegree, 0, 0, 1);
			mLHornCylinder.setCylinderInfo(density * (RobotLHornOffsetX + RobotHornRadius) + halfWidth, density * (RobotHornOffsetY + RobotHornRadius) + halfHeight, DefaultZOrder, density * RobotHornRadius, density * RobotHornHeight);
			mLHornCylinder.addRotation(RobotHornRotateDegree, 0, 0, 1);
			mRHornHemiSphere.setSphereInfo(density * (RobotRHornOffsetX + RobotHornRadius) + halfWidth, density * (RobotHornOffsetY + RobotHornRadius) + halfHeight, DefaultZOrder, density * RobotHornRadius);
			mRHornHemiSphere.addRotation(-RobotHornRotateDegree, 0, 0, 1);
			mRHornCylinder.setCylinderInfo(density * (RobotRHornOffsetX + RobotHornRadius) + halfWidth, density * (RobotHornOffsetY + RobotHornRadius) + halfHeight, DefaultZOrder, density * RobotHornRadius, density * RobotHornHeight);
			mRHornCylinder.addRotation(-RobotHornRotateDegree, 0, 0, 0.5f);
			mLEyeCircle.setCircleInfo(density * (RobotHeadOffsetX + RobotEyeOffsetX + RobotEyeRadius) + halfWidth, density * (RobotHeadOffsetY + RobotEyeOffsetY + RobotEyeRadius) + halfHeight, 50f, density * RobotEyeRadius * 2, density * RobotEyeRadius * 2);
			mREyeCircle.setCircleInfo(density * (RobotHeadOffsetX + RobotEyeOffsetX + RobotEyeRadius * 2 + RobotEyesSpace + RobotEyeRadius) + halfWidth, density * (RobotHeadOffsetY + RobotEyeOffsetY + RobotEyeRadius) + halfHeight, 50f, density * RobotEyeRadius * 2, density * RobotEyeRadius * 2);
			mFirstSurfaceCreate = false;
		}
	}

	public void swing(float degree, int axis) {
		RotateInfo swingInfo = mSwingInfo;

		if(swingInfo == null){
			swingInfo = mSwingInfo = new RotateInfo();
		}

		swingInfo.rotateDegree = degree;
		switch(axis){
		case AxisX:
			swingInfo.centerX = 1f;
			swingInfo.centerY = 0f;
			swingInfo.centerZ = 0f;
			break;
		case AxisY:
			swingInfo.centerX = 0f;
			swingInfo.centerY = 1f;
			swingInfo.centerZ = 0f;
			break;
		case AxisZ:
			swingInfo.centerX = 0f;
			swingInfo.centerY = 0f;
			swingInfo.centerZ = 1f;
			break;
		}
	}

	public void rotate(float degree, int axis) {
		RotateInfo partsRotateInfo = mPartsRotateInfo;

		if(partsRotateInfo == null){
			partsRotateInfo = mPartsRotateInfo = new RotateInfo();
			mLHornHemiSphere.addRotation(partsRotateInfo);
			mLHornCylinder.addRotation(partsRotateInfo);
			mRHornHemiSphere.addRotation(partsRotateInfo);
			mRHornCylinder.addRotation(partsRotateInfo);
			mHeadHemiSphere.addRotation(partsRotateInfo);
			mLEyeCircle.addRotation(partsRotateInfo);
			mREyeCircle.addRotation(partsRotateInfo);
		}

		partsRotateInfo.rotateDegree = degree;
		switch(axis){
		case AxisX:
			partsRotateInfo.centerX = 1f;
			partsRotateInfo.centerY = 0f;
			partsRotateInfo.centerZ = 0f;
			break;
		case AxisY:
			partsRotateInfo.centerX = 0f;
			partsRotateInfo.centerY = 1f;
			partsRotateInfo.centerZ = 0f;
			break;
		case AxisZ:
			partsRotateInfo.centerX = 0f;
			partsRotateInfo.centerY = 0f;
			partsRotateInfo.centerZ = 1f;
			break;
		}
	}

	@Override
	public void drawBase(GL10 gl) {
		boolean pushMatrix = false;
		RotateInfo swingInfo = mSwingInfo;

		try{
			if(swingInfo != null && swingInfo.rotateDegree != 0){
				gl.glPushMatrix();
				pushMatrix = true;
				gl.glRotatef(swingInfo.rotateDegree, swingInfo.centerX, swingInfo.centerY, swingInfo.centerZ);
			}

			mLHornHemiSphere.draw(gl);
			mLHornCylinder.draw(gl);
			mRHornHemiSphere.draw(gl);
			mRHornCylinder.draw(gl);
			mHeadHemiSphere.draw(gl);
			mLEyeCircle.draw(gl);
			mREyeCircle.draw(gl);
		}finally{
			if(pushMatrix){
				gl.glPopMatrix();
			}
		}
	}

	@Override
	public void release(GL10 gl) {
	}
}
