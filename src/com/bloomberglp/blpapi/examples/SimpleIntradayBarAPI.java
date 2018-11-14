package com.bloomberglp.blpapi.examples;

import com.alibaba.fastjson.JSON;
import com.bloomberglp.blpapi.*;
import com.bloomberglp.blpapi.examples.model.BarTickDataInfo;
import com.bloomberglp.blpapi.examples.model.CoreDataResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class SimpleIntradayBarAPI extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();
        String security = request.getParameter("security");
        String startDateTime = request.getParameter("startDateTime");
        String endDateTime = request.getParameter("endDateTime");
        String interval = request.getParameter("interval");


        try {
            out.println(run(security,startDateTime,endDateTime,interval));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private String run(String security,String startDateTime,String endDateTime,String interval) throws Exception
    {
        String serverHost = "localhost";
        int serverPort = 8194;

        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(serverHost);
        sessionOptions.setServerPort(serverPort);

        Session session = new Session(sessionOptions);
        if (!session.start()) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        Thread.sleep(3000);
                        String cmd = "C:\\javaweb\\xampp\\tomcat\\bin\\Java轻度服务重启.bat";
                        Runtime.getRuntime().exec(cmd).waitFor();

                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }).start();

            return  toRespose("Failed to start session.",null,-1);
        }
        if (!session.openService("//blp/refdata")) {
            return  toRespose("Failed to open //blp/refdata",null,-2);
        }

        Service refDataService = session.getService("//blp/refdata");
        Request request = refDataService.createRequest("IntradayBarRequest");
        request.set("security", security);
        request.set("eventType", "TRADE");

        if(interval!=null){
            request.set("interval", interval);    // bar interval in minutes
        }else{
            return  toRespose("interval 不能为空",null,-7);
        }

        if(startDateTime!=null){
            request.set("startDateTime",startDateTime+"T00:00:00.000" );
        }else{
            return  toRespose("startDateTime 不能为空",null,-7);
        }

        if(endDateTime!=null){
            request.set("endDateTime", endDateTime+"T00:00:00.000");
        }else{
            return  toRespose("endDateTime 不能为空",null,-7);
        }


        session.sendRequest(request, null);

        while (true) {
            Event event = session.nextEvent();
            MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();


                if(event.eventType() == Event.EventType.RESPONSE){
                    List<BarTickDataInfo> data=new ArrayList<BarTickDataInfo>();
                    try {
                        Element barData=msg.getElement("barData");
                        Element barTickData=barData.getElement("barTickData");
                        int numElements = barTickData.numValues();
                        for (int i=0; i < numElements; i++) {
                            Element field=barTickData.getValueAsElement(i);
                            BarTickDataInfo info=new BarTickDataInfo();
                            String time=field.getElementAsString("time");
                            String open=field.getElementAsString("open");
                            String high=field.getElementAsString("high");
                            String low=field.getElementAsString("low");
                            String close=field.getElementAsString("close");
                            String volume=field.getElementAsString("volume");
                            String numEvents=field.getElementAsString("numEvents");
                            String value=field.getElementAsString("value");
                            info.setTime(time);
                            info.setOpen(open);
                            info.setHigh(high);
                            info.setLow(low);
                            info.setClose(close);
                            info.setVolume(volume);
                            info.setNumEvents(numEvents);
                            info.setValue(value);
                            data.add(info);
                        }

                    }catch (Exception e){
                        return  toRespose("错误(建议检查所填写参数)："+e.getMessage(),data,0);
                    }

                    return  toRespose("请求成功",data,0);
                }
            }
            if (event.eventType() == Event.EventType.RESPONSE) {
                break;
            }
        }
        return  toRespose("未知错误",null,-4);
    }

    public String toRespose(String msg, List<BarTickDataInfo> data, int status){
        CoreDataResponse<List<BarTickDataInfo> > responseInfo = new CoreDataResponse<List<BarTickDataInfo> >();
        responseInfo.setMsg(msg);
        responseInfo.setData(data);
        responseInfo.setStatus(status);
        String respose=JSON.toJSONString(responseInfo);
        return  respose;
    }


}
