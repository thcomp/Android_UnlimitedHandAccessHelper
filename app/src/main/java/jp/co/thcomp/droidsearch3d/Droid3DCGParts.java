package jp.co.thcomp.droidsearch3d;

import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

import jp.co.thcomp.glsurfaceview.GLDrawView;
import jp.co.thcomp.glsurfaceview.GLViewSpace;
import jp.co.thcomp.glsurfaceview.RotateInfo;
import jp.co.thcomp.glsurfaceview.ScaleInfo;
import jp.co.thcomp.glsurfaceview.TranslateInfo;
import jp.co.thcomp.util.DisplayUtil;

abstract public class Droid3DCGParts{
	static final int AxisX = 0;
	static final int AxisY = 1;
	static final int AxisZ = 2;

	protected static final int RobotColor = 0x00A4C639;
	protected static final float RobotColorR[] = {((float)((RobotColor & 0x00FF0000) >> 16)) / 0xFF};
	protected static final float RobotColorG[] = {((float)((RobotColor & 0x0000FF00) >> 8)) / 0xFF};
	protected static final float RobotColorB[] = {((float)((RobotColor & 0x000000FF) >> 0)) / 0xFF};
	protected static final float RobotColorA[] = {1.0f};
	protected static final float RobotEyeColorR[] = {1f};
	protected static final float RobotEyeColorG[] = {1f};
	protected static final float RobotEyeColorB[] = {1f};
	protected static final float RobotEyeColorA[] = RobotColorA;
	protected static final int DefaultZOrder = 0;
	protected static final float RobotMargin = 2f;
	protected static final float RobotWidth = 99f;
	protected static final float RobotHeight = 116f;
	protected static final float RobotHeadWidth = 65f;
	protected static final float RobotHeadRadius = RobotHeadWidth / 2;
	protected static final float RobotHeadHeight = RobotHeadWidth / 2f;	// correct size is 31f;
	protected static final float RobotHeadOffsetX = 19f/* - RobotMargin*/;
	protected static final float RobotHeadOffsetY = 8f/* - RobotMargin*/;
	protected static final float RobotEyeOffsetX = 15f;	// offset from head side edge
	protected static final float RobotEyeOffsetY = 13f;	// offset from head top edge
	protected static final float RobotEyesSpace = 23f;
	protected static final float RobotEyeRadius = 3f/*(RobotHeadWidth - RobotEyeOffsetX * 2f - RobotEyesSpace) / 4f*/;
	protected static final float RobotHornRotateDegree = 29f;
	protected static final float RobotHornAngle = (float) (2 * Math.PI * (RobotHornRotateDegree / 360));
	protected static final float RobotLHornOffsetX = RobotHeadOffsetX + RobotEyeOffsetX + RobotEyeRadius - 6;
	protected static final float RobotRHornOffsetX = RobotHeadOffsetX + RobotEyeOffsetX + RobotEyeRadius * 2 + RobotEyesSpace + RobotEyeRadius + 6;
	protected static final float RobotHornOffsetY = 1;
	protected static final float RobotHornWidth = 2f;
	protected static final float RobotHornHeight = 12f;
	protected static final float RobotHornRadius = RobotHornWidth / 2;
	protected static final float RobotSpaceHeadAndBody = 1f;
	protected static final float RobotSpaceArmAndBody = 2f;
	protected static final float RobotBodyWidth = 65f;
	protected static final float RobotBodyHeight = 55f;
	protected static final float RobotBodyOffsetX = RobotHeadOffsetX;
	protected static final float RobotBodyOffsetY = RobotHeadOffsetY + RobotHeadHeight + RobotSpaceHeadAndBody;
	protected static final float RobotBodyRadius = RobotHeadRadius;
	protected static final float RobotArmWidth = 15f;
	protected static final float RobotArmHeight = 45f;
	protected static final float RobotArmRadius = RobotArmWidth / 2f;
	protected static final float RobotArmCylinderHeight = RobotArmHeight - RobotArmRadius * 2;
	protected static final float RobotLArmOffsetX = RobotMargin;
	//protected static final float RobotRArmOffsetX = RobotWidth - RobotMargin - RobotArmWidth;
	protected static final float RobotRArmOffsetX = RobotLArmOffsetX + RobotArmWidth + RobotSpaceArmAndBody + RobotBodyWidth + RobotSpaceArmAndBody;
	protected static final float RobotArmOffsetY = RobotHeadOffsetY + RobotHeadHeight;
	protected static final float RobotFootWidth = 15f;
	protected static final float RobotFootHeight = 23f;
	protected static final float RobotFootRadius = RobotFootWidth / 2f;
	protected static final float RobotFootCylinderHeight = RobotFootHeight - RobotFootRadius;
	protected static final float RobotFootOffsetXFromBodyEdge = 13f;
	protected static final float RobotLFootOffsetX = RobotBodyOffsetX + RobotFootOffsetXFromBodyEdge;
	protected static final float RobotRFootOffsetX = RobotBodyOffsetX + RobotBodyWidth - RobotFootOffsetXFromBodyEdge - RobotFootWidth;
	protected static final float RobotFootOffsetY = RobotHeadOffsetY + RobotHeadHeight + RobotSpaceHeadAndBody + RobotBodyHeight;

	protected int mSurfaceWidth;
	protected int mSurfaceHeight;
	protected Context mContext;
	protected GLDrawView mView;
	protected TranslateInfo mAnimeTranslateInfo = null;
	protected RotateInfo mAnimeRotateInfo = null;
	protected ScaleInfo mAnimeScaleInfo = null;

	public static final float getRobotWidth(Context context){
		return DisplayUtil.getDefaultDisplayDensity(context) * RobotWidth/* * RobotMargin * 2*/;
	}

	public static final float getRobotHeight(Context context){
		return DisplayUtil.getDefaultDisplayDensity(context) * RobotHeight/* * RobotMargin * 2*/;
	}

	public Droid3DCGParts(GLDrawView view) {
		mView = view;
		mContext = view.getContext();
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;
	}

	public final void draw(GL10 gl){
		gl.glPushMatrix();
		GLViewSpace viewSpace = mView.getViewSpace();
		{
			RotateInfo baseRotateInfo = mAnimeRotateInfo;
			TranslateInfo baseTranslateInfo = mAnimeTranslateInfo;
			ScaleInfo baseScaleInfo = mAnimeScaleInfo;
			if(baseTranslateInfo != null){
				if(baseTranslateInfo.translateByXWR == Float.MAX_VALUE){
					baseTranslateInfo.translateByXWR = viewSpace.changeViewPortXtoWorldReferenceX(baseTranslateInfo.translateByX);
				}
				if(baseTranslateInfo.translateByYWR == Float.MAX_VALUE){
					baseTranslateInfo.translateByYWR = viewSpace.changeViewPortYtoWorldReferenceY(baseTranslateInfo.translateByY);
				}
				if(baseTranslateInfo.translateByZWR == Float.MAX_VALUE){
					baseTranslateInfo.translateByZWR = viewSpace.changeViewPortZtoWorldReferenceZ(baseTranslateInfo.translateByZ);
				}
				gl.glTranslatef(baseTranslateInfo.translateByXWR, baseTranslateInfo.translateByYWR, baseTranslateInfo.translateByZWR);
			}
			if(baseRotateInfo != null && baseRotateInfo.rotateDegree % 360 != 0){
				gl.glRotatef(baseRotateInfo.rotateDegree, baseRotateInfo.centerX, baseRotateInfo.centerY, baseRotateInfo.centerZ);
			}
			if(baseScaleInfo != null){
				if(baseScaleInfo.scaleXWR == Float.MAX_VALUE){
					baseScaleInfo.scaleXWR = viewSpace.changeViewPortSizeXtoWorldReferenceSizeX(baseScaleInfo.scaleX);
				}
				if(baseScaleInfo.scaleYWR == Float.MAX_VALUE){
					baseScaleInfo.scaleYWR = viewSpace.changeViewPortSizeYtoWorldReferenceSizeY(baseScaleInfo.scaleY);
				}
				if(baseScaleInfo.scaleZWR == Float.MAX_VALUE){
					baseScaleInfo.scaleZWR = viewSpace.changeViewPortSizeZtoWorldReferenceSizeZ(baseScaleInfo.scaleZ);
				}
				gl.glScalef(baseScaleInfo.scaleXWR, baseScaleInfo.scaleYWR, baseScaleInfo.scaleZWR);
			}
		}

		drawBase(gl);

		gl.glPopMatrix();
	}

	abstract public void swing(float degree, int axis);
	abstract public void rotate(float degree, int axis);
	abstract public void drawBase(GL10 gl);
	abstract public void release(GL10 gl);
}
