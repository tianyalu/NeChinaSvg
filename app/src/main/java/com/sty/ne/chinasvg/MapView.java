package com.sty.ne.chinasvg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.graphics.PathParser;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by tian on 2019/10/28.
 */

public class MapView extends View {
    private int[] colorArray = new int[]{0xFF239BD7, 0xFF30A9E5, 0xFF80CBF1, 0xFFFFFFFF};
    private Context context;
    private List<ProvinceItem> provinceItemList;
    private Paint paint;
    private ProvinceItem select;
    private RectF totalRect;
    private float scale = 1.0f;

    public MapView(Context context) {
        this(context, null);
    }

    public MapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        this.context = context;
        paint = new Paint();
        paint.setAntiAlias(true);
        provinceItemList = new ArrayList<>();

        loadThread.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //获取当前控件宽高值
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if(totalRect != null) {
            double mapWidth = totalRect.width();
            scale = (float) (width / mapWidth); // 屏幕宽度/包裹地图的最小矩形宽度
        }

        setMeasuredDimension(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    private Thread loadThread = new Thread(){

        @Override
        public void run() {
            InputStream inputStream = context.getResources().openRawResource(R.raw.china);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); //获取DocumentBuilderFactory实例
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder(); //从factory获取DocumentBuilder实例
                Document doc = builder.parse(inputStream); //解析输入流得到Document实例
                Element rootElement = doc.getDocumentElement();
                NodeList items = rootElement.getElementsByTagName("path");
                float left = -1;
                float right = -1;
                float top = -1;
                float bottom = -1;
                List<ProvinceItem> list = new ArrayList<>();

                for (int i = 0; i < items.getLength(); i++) {
                    Element element = (Element) items.item(i);
                    String pathData = element.getAttribute("android:pathData");
                    @SuppressLint("RestrictedApi") Path path = PathParser.createPathFromPathData(pathData);
                    ProvinceItem provinceItem = new ProvinceItem(path);
                    provinceItem.setDrawColor(colorArray[i % 4]);

                    RectF rectF = new RectF();
                    path.computeBounds(rectF, true);
                    left = left == -1 ? rectF.left : Math.min(left, rectF.left);
                    right = right == -1 ? rectF.right : Math.max(right, rectF.right);
                    top = top == -1 ? rectF.top : Math.min(top, rectF.top);
                    bottom = bottom == -1 ? rectF.bottom : Math.max(bottom, rectF.bottom);

                    list.add(provinceItem);
                }
                provinceItemList = list;
                //得到包含所有省份矩形的最大矩形
                totalRect = new RectF(left, top, right, bottom);

                //刷新界面
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        requestLayout();
                        invalidate();
                    }
                });
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(provinceItemList != null) {
            canvas.save();
            //绘制的缩放处理
            canvas.scale(scale, scale);
            for (ProvinceItem provinceItem : provinceItemList) {
                if(provinceItem != select) {
                    provinceItem.drawItem(canvas, paint, true);
                }else {
                    provinceItem.drawItem(canvas, paint, false);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //点击事件的缩放处理
        handleTouch(event.getX() / scale, event.getY() / scale);
        return super.onTouchEvent(event);
    }

    private void handleTouch(float x, float y) {
        if(provinceItemList == null) {
            return;
        }
        ProvinceItem selectItem = null;
        for (ProvinceItem provinceItem : provinceItemList) {
            if(provinceItem.isTouch(x, y)) {
                selectItem = provinceItem;
            }
        }
        if(selectItem != null) {
            select = selectItem;
            postInvalidate();
        }
    }
}
