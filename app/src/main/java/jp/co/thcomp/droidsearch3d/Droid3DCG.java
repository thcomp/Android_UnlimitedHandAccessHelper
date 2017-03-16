package jp.co.thcomp.droidsearch3d;

import android.content.Context;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import jp.co.thcomp.glsurfaceview.GLDrawView;
import jp.co.thcomp.glsurfaceview.GLViewSpace;
import jp.co.thcomp.glsurfaceview.RotateInfo;
import jp.co.thcomp.glsurfaceview.TranslateInfo;
import jp.co.thcomp.util.Constant;

public class Droid3DCG extends BaseDroid3DCG {
    private static final float LightAmbient[] = {0.f, 0.f, 0.f, 1.f};
    private static final float LightDiffuse[] = {1.f, 1.f, 1.f, 1.f};
    private static final float LightSpecular[] = {1.f, 1.f, 1.f, 1.f};
    private static final float LightPosition[] = {0.f, 1.f, 0.f, 0.f};
    private TranslateInfo mDroidPosInfo = null;
    private HashMap<Integer, RotateInfo> mDroidRotateInfoMap = new HashMap<Integer, RotateInfo>();
    private FloatBuffer mLightAmbient;
    private FloatBuffer mLightDiffuse;
    private FloatBuffer mLightSpecular;
    private FloatBuffer mLightPosition;

    public Droid3DCG(final GLDrawView view) {
        super(view);

        mLightAmbient = ByteBuffer.allocateDirect(4 * Constant.FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mLightDiffuse = ByteBuffer.allocateDirect(4 * Constant.FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mLightSpecular = ByteBuffer.allocateDirect(4 * Constant.FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mLightPosition = ByteBuffer.allocateDirect(4 * Constant.FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mLightAmbient.put(LightAmbient);
        mLightDiffuse.put(LightDiffuse);
        mLightSpecular.put(LightSpecular);
        mLightPosition.put(LightPosition);
    }

    public void moveTo(int posX, int posY, int posZ) {
        moveTo(posX, posY);
        mDroidPosInfo.translateByZ = posZ;
        mDroidPosInfo.translateByZWR = Float.MAX_VALUE;
    }

    public void moveTo(int posX, int posY) {
        TranslateInfo translateInfo = mDroidPosInfo;
        if (translateInfo == null) {
            translateInfo = mDroidPosInfo = new TranslateInfo();
        }

        translateInfo.translateByX = posX;
        translateInfo.translateByY = posY;
        translateInfo.translateByXWR = Float.MAX_VALUE;
        translateInfo.translateByYWR = Float.MAX_VALUE;
    }

    public float getDroidWidth(Context context) {
        return Droid3DCGParts.getRobotWidth(context);
    }

    public float getDroidHeight(Context context) {
        return Droid3DCGParts.getRobotHeight(context);
    }

    public void rotateDroid(float degree, int axis) {
        RotateInfo droidRotateInfo = mDroidRotateInfoMap.get(axis);
        if (droidRotateInfo == null) {
            droidRotateInfo = new RotateInfo();
            switch (axis) {
                case Droid3DCG.AxisX:
                    droidRotateInfo.centerX = 1;
                    droidRotateInfo.centerY = 0;
                    droidRotateInfo.centerZ = 0;
                    break;
                case Droid3DCG.AxisZ:
                    droidRotateInfo.centerX = 0;
                    droidRotateInfo.centerY = 0;
                    droidRotateInfo.centerZ = 1;
                    break;
                case Droid3DCG.AxisY:
                default:
                    droidRotateInfo.centerX = 0;
                    droidRotateInfo.centerY = 1;
                    droidRotateInfo.centerZ = 0;
                    break;
            }
            mDroidRotateInfoMap.put(axis, droidRotateInfo);
        }

        droidRotateInfo.rotateDegree = degree;
        mView.requestRender();
    }

    @Override
    public void draw(GL10 gl) {
        boolean needPopMatrix = false;

        try {
            TranslateInfo translateInfo = mDroidPosInfo;
            if (translateInfo != null) {
                GLViewSpace viewSpace = mView.getViewSpace();
                needPopMatrix = true;

                gl.glPushMatrix();
                if (translateInfo.translateByXWR == Float.MAX_VALUE) {
                    translateInfo.translateByXWR = viewSpace.changeViewPortXtoWorldReferenceX(translateInfo.translateByX);
                }
                if (translateInfo.translateByYWR == Float.MAX_VALUE) {
                    translateInfo.translateByYWR = viewSpace.changeViewPortYtoWorldReferenceY(translateInfo.translateByY);
                }
                if (translateInfo.translateByZWR == Float.MAX_VALUE) {
                    translateInfo.translateByZWR = viewSpace.changeViewPortZtoWorldReferenceZ(translateInfo.translateByZ);
                }
                gl.glTranslatef(translateInfo.translateByXWR, translateInfo.translateByYWR, translateInfo.translateByZWR);
            }

            for (RotateInfo rotateInfo : mDroidRotateInfoMap.values()) {
                if (rotateInfo != null) {
                    if (!needPopMatrix) {
                        needPopMatrix = true;
                        gl.glPushMatrix();
                    }
                    gl.glRotatef(rotateInfo.rotateDegree, rotateInfo.centerX, rotateInfo.centerY, rotateInfo.centerZ);
                }
            }

            //gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, mLightAmbient);
            //gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, mLightDiffuse);
            //gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, mLightSpecular);
            //gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, mLightPosition);

            //gl.glEnable(GL10.GL_LIGHT0);
            //gl.glEnable(GL10.GL_LIGHTING);
            //gl.glEnable(GL10.GL_NORMALIZE);
            //gl.glNormal3f(0f, 1f, 0f);

            super.draw(gl);
        } finally {
            if (needPopMatrix) {
                gl.glPopMatrix();
            }
        }
    }

}
