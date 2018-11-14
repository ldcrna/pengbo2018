package com.bloomberglp.blpapi.examples;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import com.alibaba.fastjson.JSON;
import com.bloomberglp.blpapi.*;
import com.bloomberglp.blpapi.examples.model.BarTickDataInfo;
import com.bloomberglp.blpapi.examples.model.CoreDataResponse;
import com.bloomberglp.blpapi.examples.model.HistoryInfo;
import com.bloomberglp.blpapi.examples.model.TickDataInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SimpleIntradayTickAPI extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();

        String security = request.getParameter("security");
        String startDateTime = request.getParameter("startDateTime");
        String endDateTime = request.getParameter("endDateTime");
        String interval = request.getParameter("interval");

        try {
            out.println(run(security, startDateTime, endDateTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String run(String security, String startDateTime, String endDateTime) throws Exception {
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

            return toRespose("Failed to start session.", null, -1);
        }
        if (!session.openService("//blp/refdata")) {
            return toRespose("Failed to open //blp/refdata", null, -2);
        }
        Service refDataService = session.getService("//blp/refdata");
        Request request = refDataService.createRequest("IntradayTickRequest");
        request.set("security", security);
        request.getElement("eventTypes").appendValue("TRADE");
        request.getElement("eventTypes").appendValue("AT_TRADE");

        request.set("startDateTime", startDateTime);
        request.set("endDateTime", endDateTime);
        request.set("includeConditionCodes", true);

        session.sendRequest(request, null);

        while (true) {
            Event event = session.nextEvent();
            MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();


                if (event.eventType() == Event.EventType.RESPONSE) {
                    List<TickDataInfo> data = new ArrayList<TickDataInfo>();

                    try {


                        Element tickData = msg.getElement("tickData");
                        Element barTickData = tickData.getElement("tickData");
                        int numElements = barTickData.numValues();
                        for (int i = 0; i < numElements; i++) {
                            Element field = barTickData.getValueAsElement(i);
                            TickDataInfo info = new TickDataInfo();
                            String time = field.getElementAsString("time");
                            String type = field.getElementAsString("type");
                            String value = field.getElementAsString("value");
                            String size = field.getElementAsString("size");

                            info.setTime(time);
                            info.setType(type);
                            info.setValue(value);
                            info.setSize(size);

                            try {
                                String conditionCodes = field.getElementAsString("conditionCodes");
                                info.setConditionCodes(conditionCodes);
                            } catch (Exception e) {

                            } finally {
                                data.add(info);
                            }
                        }
                    } catch (Exception e) {
                        return toRespose(e.getMessage(), null, -6);
                    }

                    return toRespose("请求成功", data, 0);
                }

            }
            if (event.eventType() == Event.EventType.RESPONSE) {
                break;
            }
        }
        return toRespose("未知错误", null, -4);
    }

    public String toRespose(String msg, List<TickDataInfo> data, int status) {
        CoreDataResponse<List<TickDataInfo>> responseInfo = new CoreDataResponse<List<TickDataInfo>>();
        responseInfo.setMsg(msg);
        responseInfo.setData(data);
        responseInfo.setStatus(status);
        String respose = JSON.toJSONString(responseInfo);
        return respose;
    }


}
