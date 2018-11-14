package com.bloomberglp.blpapi.examples;

import com.alibaba.fastjson.JSON;
import com.bloomberglp.blpapi.*;
import com.bloomberglp.blpapi.examples.model.CoreDataResponse;
import com.bloomberglp.blpapi.examples.model.HistoryInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SimpleHistoryAPI extends HttpServlet {


    private static final Name SECURITY_DATA = new Name("securityData");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name FIELD_DATE = new Name("date");
    private static final Name FIELD_PX_OPEN = new Name("PX_OPEN");
    private static final Name FIELD_PX_HIGH = new Name("PX_HIGH");
    private static final Name FIELD_PX_LOW = new Name("PX_LOW");
    private static final Name FIELD_PX_LAST = new Name("PX_LAST");
    private static final Name FIELD_PX_VOLUME = new Name("PX_VOLUME");

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();

//        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
//        out.println("<HTML>");
//        out.println("  <HEAD><TITLE>A Servlet</TITLE>  </HEAD>");
//        out.println("  <BODY>");


        String security = request.getParameter("security");
        String startTime = request.getParameter("startTime");
        String endTime = request.getParameter("endTime");
        String periodicitySelection = request.getParameter("periodicitySelection");
        String maxDataPoints = request.getParameter("maxDataPoints");

        //SimpleHistoryAPI example = new SimpleHistoryAPI();
        try {
            out.println(run(security, startTime, endTime, periodicitySelection, maxDataPoints));
        } catch (Exception e) {
            e.printStackTrace();
        }

//        out.println("  </BODY>");cls
//        out.println("</HTML>");
        out.flush();
        out.close();
    }

    private String deBugMsg="";
    private boolean isDeBug=true;

    public String toRespose(String msg, List<HistoryInfo> data, int status) {
        CoreDataResponse<List<HistoryInfo>> responseInfo = new CoreDataResponse<List<HistoryInfo>>();
        responseInfo.setMsg(msg+deBugMsg);
        responseInfo.setData(data);
        responseInfo.setStatus(status);
        String respose = JSON.toJSONString(responseInfo);
        return respose;
    }


    private String run(String security, String startTime, String endTime, String periodicitySelection, String maxDataPoints) throws Exception {
        String serverHost = "localhost";
        int serverPort = 8194;
        CoreDataResponse<List<HistoryInfo>> responseInfo = new CoreDataResponse<List<HistoryInfo>>();

        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(serverHost);
        sessionOptions.setServerPort(serverPort);

        Session session = new Session(sessionOptions);
        if (!session.start()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
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
            System.err.println("Failed to open //blp/refdata");
            return toRespose("Failed to open //blp/refdata", null, -2);
        }

        Service refDataService = session.getService("//blp/refdata");
        Request request = refDataService.createRequest("HistoricalDataRequest");

        Element securities = request.getElement("securities");
        if (security!=null)
        {
            securities.appendValue(security);

        }else{
            return toRespose("security 不能为空", null, -7);
        }

        Element fields = request.getElement("fields");
        fields.appendValue("PX_OPEN");
        fields.appendValue("PX_HIGH");
        fields.appendValue("PX_LOW");
        fields.appendValue("PX_LAST");
        fields.appendValue("PX_VOLUME");

        if (startTime!=null){
            request.set("startDate", startTime);
        }
        else{
            return toRespose("startTime 不能为空", null, -7);
        }
        if (endTime!=null)
            request.set("endDate", endTime);
        if (maxDataPoints!=null)
            request.set("maxDataPoints", maxDataPoints);
        if (periodicitySelection!=null){
            request.set("periodicitySelection", periodicitySelection);
        }
        else{
            return toRespose("periodicitySelection 不能为空", null, -7);
        }
        //request.set("returnEids", true);

        session.sendRequest(request, null);

        while (true) {
            Event event = session.nextEvent();
            MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();


                if (event.eventType() == Event.EventType.RESPONSE) {


                    if(isDeBug){
                        //deBugMsg=""+msg;
//                      deBugMsg=msg.getElementAsString(SECURITY_DATA);
                    }
                    List<HistoryInfo> historyInfos = new ArrayList<HistoryInfo>();

                    try {

                        if (msg.hasElement(SECURITY_DATA)) {
                            Element securityData = msg.getElement("securityData");

                            Element fieldData = securityData.getElement(FIELD_DATA);
                            int numElements = fieldData.numValues();

                            for (int i = 0; i < numElements; i++) {

                                HistoryInfo info = new HistoryInfo();
                                Element field = fieldData.getValueAsElement(i);
                                if (field.hasElement(FIELD_DATE)) {
                                    String date = field.getElementAsString(FIELD_DATE);
                                    info.setDate(date);
                                }

                                if (field.hasElement(FIELD_PX_OPEN)) {
                                    String PX_OPEN = field.getElementAsString(FIELD_PX_OPEN);
                                    info.setPX_OPEN(PX_OPEN);
                                }

                                if (field.hasElement(FIELD_PX_HIGH)) {
                                    String PX_HIGH = field.getElementAsString(FIELD_PX_HIGH);
                                    info.setPX_HIGH(PX_HIGH);
                                }


                                if (field.hasElement(FIELD_PX_LOW)) {
                                    String PX_LOW = field.getElementAsString(FIELD_PX_LOW);
                                    info.setPX_LOW(PX_LOW);
                                }

                                if (field.hasElement(FIELD_PX_LAST)) {
                                    String PX_LAST = field.getElementAsString(FIELD_PX_LAST);
                                    info.setPX_LAST(PX_LAST);
                                }

                                if (field.hasElement(FIELD_PX_VOLUME)) {
                                    String PX_VOLUME = field.getElementAsString(FIELD_PX_VOLUME);
                                    info.setPX_VOLUME(PX_VOLUME);
                                }

                                historyInfos.add(info);
                            }

                        }
                        return toRespose("请求成功", historyInfos, 0);

                    } catch (Exception e) {
                        deBugMsg=deBugMsg+e.getMessage();
                        toRespose("数据转化异常", null, -3);
                    }
                    return toRespose("请求成功", historyInfos, 0);
                }
            }
            if (event.eventType() == Event.EventType.RESPONSE) {
                break;
            }

        }

        return toRespose("未知错误", null, -4);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    public SimpleHistoryAPI() {
        super();
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
