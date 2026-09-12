package com.fatih.hermesbots;
import android.content.Context;
import android.graphics.*;
import android.view.View;

final class IconView extends View {
 private final String type; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 IconView(Context c,String type){super(c);this.type=type;}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();float unit=Math.min(getWidth(),getHeight())/48f;canvas.translate(getWidth()/2f-24*unit,getHeight()/2f-24*unit);canvas.scale(unit,unit);p.setColor(0xfff3f3f4);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
 switch(type){
 case "plus":canvas.drawLine(24,15,24,33,p);canvas.drawLine(15,24,33,24,p);break;
 case "back":canvas.drawLine(28,15,19,24,p);canvas.drawLine(19,24,28,33,p);break;
 case "more":p.setStyle(Paint.Style.FILL);for(int x=16;x<=32;x+=8)canvas.drawCircle(x,24,1.7f,p);break;
 case "send":p.setStyle(Paint.Style.FILL);canvas.drawCircle(24,24,19,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xff161719);canvas.drawLine(24,32,24,16,p);canvas.drawLine(24,16,18,22,p);canvas.drawLine(24,16,30,22,p);break;
 case "chat":canvas.drawRoundRect(11,12,37,32,6,6,p);canvas.drawLine(17,32,14,37,p);canvas.drawLine(14,37,25,32,p);break;
 case "bots":canvas.drawRoundRect(11,15,37,35,6,6,p);canvas.drawLine(24,10,24,15,p);canvas.drawCircle(19,24,1,p);canvas.drawCircle(29,24,1,p);canvas.drawLine(20,30,28,30,p);break;
 case "settings":canvas.drawCircle(24,24,9,p);canvas.drawCircle(24,24,3,p);for(int i=0;i<8;i++){canvas.save();canvas.rotate(45*i,24,24);canvas.drawLine(24,11,24,15,p);canvas.restore();}break;
 }
 canvas.restore();
 }
}
