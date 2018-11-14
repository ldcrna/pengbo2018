package com.bloomberglp.blpapi.examples;

import com.alibaba.fastjson.JSON;
import com.bloomberglp.blpapi.*;
import com.bloomberglp.blpapi.examples.model.CoreDataResponse;
import com.bloomberglp.blpapi.examples.model.SecurityLookupInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;


public class SecurityLookupAPI extends HttpServlet {

    private static final Name AUTHORIZATION_SUCCESS = Name.getName("AuthorizationSuccess");
    private static final Name TOKEN_SUCCESS = Name.getName("TokenGenerationSuccess");
    private static final Name SESSION_TERMINATED = Name.getName("SessionTerminated");
    private static final Name SESSION_FAILURE = Name.getName("SessionStartupFailure");
    private static final Name TOKEN_ELEMENT = Name.getName("token");
    private static final Name DESCRIPTION_ELEMENT = Name.getName("description");
    private static final Name QUERY_ELEMENT = Name.getName("query");
    private static final Name RESULTS_ELEMENT = Name.getName("results");
    private static final Name MAX_RESULTS_ELEMENT = Name.getName("maxResults");

    private static final Name SECURITY_ELEMENT = Name.getName("security");

    private static final Name ERROR_RESPONSE = Name.getName("ErrorResponse");
    private static final Name INSTRUMENT_LIST_RESPONSE = Name.getName("InstrumentListResponse");
    private static final Name CURVE_LIST_RESPONSE = Name.getName("CurveListResponse");
    private static final Name GOVT_LIST_RESPONSE = Name.getName("GovtListResponse");

    private static final Name INSTRUMENT_LIST_REQUEST = Name.getName("instrumentListRequest");

    private static final String AUTH_USER = "AuthenticationType=OS_LOGON";
    private static final String AUTH_APP_PREFIX =
            "AuthenticationMode=APPLICATION_ONLY;ApplicationAuthenticationType=APPNAME_AND_KEY;"
                    + "ApplicationName=";
    private static final String AUTH_USER_APP_PREFIX =
            "AuthenticationMode=USER_AND_APPLICATION;AuthenticationType=OS_LOGON;"
                    + "ApplicationAuthenticationType=APPNAME_AND_KEY;ApplicationName=";
    private static final String AUTH_DIR_PREFIX =
            "AuthenticationType=DIRECTORY_SERVICE;DirSvcPropertyName=";
    private static final String AUTH_OPTION_NONE = "none";
    private static final String AUTH_OPTION_USER = "user";
    private static final String AUTH_OPTION_APP = "app=";
    private static final String AUTH_OPTION_USER_APP = "userapp=";
    private static final String AUTH_OPTION_DIR = "dir=";
    private static final String INSTRUMENT_SERVICE = "//blp/instruments";
    private static final String AUTH_SERVICE = "//blp/apiauth";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8194;
   // private static final String DEFAULT_QUERY_STRING = "IBM";
    private static final int DEFAULT_MAX_RESULTS = 10;
    private static final int WAIT_TIME_MS = 10 * 1000; // 10 seconds

    private static final String[] FILTERS_INSTRUMENTS = {
            "yellowKeyFilter",
            "languageOverride"
    };

    private static final String FILTERS_GOVT[] = {
            "ticker",
            "partialMatch"
    };

    private static final String FILTERS_CURVE[] = {
            "countryCode",
            "currencyCode",
            "type",
            "subtype",
            "curveid",
            "bbgid"
    };

    private static final Name CURVE_ELEMENT = Name.getName("curve");
    private static final Name[] CURVE_RESPONSE_ELEMENTS = {
            Name.getName("country"),
            Name.getName("currency"),
            Name.getName("curveid"),
            Name.getName("type"),
            Name.getName("subtype"),
            Name.getName("publisher"),
            Name.getName("bbgid")
    };

    private static final Name PARSEKY_ELEMENT = Name.getName("parseky");
    private static final Name NAME_ELEMENT = Name.getName("name");
    private static final Name TICKER_ELEMENT = Name.getName("ticker");

    private String d_queryString;
    private String d_host = DEFAULT_HOST;
    private Name d_requestType = INSTRUMENT_LIST_REQUEST;
    private int d_port = DEFAULT_PORT;
    private int d_maxResults = DEFAULT_MAX_RESULTS;
    private String d_authOptions = null;
    private HashMap<String, String> d_filters = new HashMap<String, String>();





    // Authorize should be called before any requests are sent.
    public static void authorize(Identity identity, Session session) throws Exception {
        if (!session.openService(AUTH_SERVICE)) {
            throw new Exception(
                    String.format("Failed to open auth service: %1$s",
                            AUTH_SERVICE));
        }
        Service authService = session.getService(AUTH_SERVICE);

        EventQueue tokenEventQueue = new EventQueue();
        session.generateToken(new CorrelationID(tokenEventQueue), tokenEventQueue);
        String token = null;
        // Generate token responses will come on the dedicated queue. There would be no other
        // messages on that queue.
        Event event = tokenEventQueue.nextEvent(WAIT_TIME_MS);

        if (event.eventType() == Event.EventType.TOKEN_STATUS
                || event.eventType() == Event.EventType.REQUEST_STATUS) {
            for (Message msg : event) {
                System.out.println(msg);
                if (msg.messageType() == TOKEN_SUCCESS) {
                    token = msg.getElementAsString(TOKEN_ELEMENT);
                }
            }
        }
        if (token == null) {
            throw new Exception("Failed to get token");
        }

        Request authRequest = authService.createAuthorizationRequest();
        authRequest.set(TOKEN_ELEMENT, token);

        session.sendAuthorizationRequest(authRequest, identity, null);

        long waitDuration = WAIT_TIME_MS;
        for (long startTime = System.currentTimeMillis();
             waitDuration > 0;
             waitDuration -= (System.currentTimeMillis() - startTime)) {
            event = session.nextEvent(waitDuration);
            // Since no other requests were sent using the session queue, the response can
            // only be for the Authorization request
            if (event.eventType() != Event.EventType.RESPONSE
                    && event.eventType() != Event.EventType.PARTIAL_RESPONSE
                    && event.eventType() != Event.EventType.REQUEST_STATUS) {
                continue;
            }

            for (Message msg : event) {
                System.out.println(msg);
                if (msg.messageType() != AUTHORIZATION_SUCCESS) {
                    throw new Exception("Authorization Failed");
                }
            }
            return;
        }
        throw new Exception("Authorization Failed");
    }

    public String toRespose(String msg, List<SecurityLookupInfo> data, int status) {
        CoreDataResponse<List<SecurityLookupInfo>> responseInfo = new CoreDataResponse<List<SecurityLookupInfo>>();
        responseInfo.setMsg(msg);
        responseInfo.setData(data);
        responseInfo.setStatus(status);
        String respose = JSON.toJSONString(responseInfo);
        return respose;
    }

    //输出
    private String processInstrumentListResponse(Message msg) {
        Element results = msg.getElement(RESULTS_ELEMENT);
        int numResults = results.numValues();
        // System.out.println("Processing " + numResults + " results:");

        List<SecurityLookupInfo> data = new ArrayList<SecurityLookupInfo>();
        for (int i = 0; i < numResults; ++i) {

            SecurityLookupInfo info = new SecurityLookupInfo();
            Element result = results.getValueAsElement(i);

            String security = result.getElementAsString(SECURITY_ELEMENT);
            String description = result.getElementAsString(DESCRIPTION_ELEMENT);
            info.setSecurity(security);
            info.setDescription(description);
            data.add(info);
        }
        return toRespose("请求成功", data, 0);
    }

    private String processCurveListResponse(Message msg) {
        Element results = msg.getElement(RESULTS_ELEMENT);
        int numResults = results.numValues();

        List<SecurityLookupInfo> data = new ArrayList<SecurityLookupInfo>();
        for (int i = 0; i < numResults; ++i) {
            Element result = results.getValueAsElement(i);
            StringBuilder sb = new StringBuilder();
            for (Name n : CURVE_RESPONSE_ELEMENTS) {
                if (sb.length() != 0) {
                    sb.append(" ");
                }
                sb.append(n).append("=").append(result.getElementAsString(n));
            }

            SecurityLookupInfo info = new SecurityLookupInfo();
            String security = result.getElementAsString(SECURITY_ELEMENT);
            String description = result.getElementAsString(DESCRIPTION_ELEMENT);
            info.setSecurity(security);
            info.setDescription(description);
            data.add(info);
            System.out.printf(
                    "\t%1$d %2$s - %3$s '%4$s'\n",
                    i + 1,
                    result.getElementAsString(CURVE_ELEMENT),
                    result.getElementAsString(DESCRIPTION_ELEMENT),
                    sb.toString());
        }

        return toRespose("请求成功", data, 0);
    }

    private String processGovtListResponse(Message msg) {
        Element results = msg.getElement(RESULTS_ELEMENT);
        int numResults = results.numValues();
        List<SecurityLookupInfo> data = new ArrayList<SecurityLookupInfo>();
        for (int i = 0; i < numResults; ++i) {
            Element result = results.getValueAsElement(i);
            SecurityLookupInfo info = new SecurityLookupInfo();
            String security = result.getElementAsString(SECURITY_ELEMENT);
            String description = result.getElementAsString(DESCRIPTION_ELEMENT);
            String ticker = result.getElementAsString(TICKER_ELEMENT);
            info.setSecurity(security);
            info.setDescription(description);
            info.setTicker(ticker);
            data.add(info);
        }
        return toRespose("请求成功", data, 0);
    }

    private String processResponseEvent(Event event) {
        MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
            Message msg = msgIter.next();
            if (msg.messageType() == ERROR_RESPONSE) {
                String description = msg.getElementAsString(DESCRIPTION_ELEMENT);
                return toRespose("Received error:", null, -2);
            } else if (msg.messageType() == INSTRUMENT_LIST_RESPONSE) {
                return processInstrumentListResponse(msg);
            } else if (msg.messageType() == CURVE_LIST_RESPONSE) {
                return processCurveListResponse(msg);
            } else if (msg.messageType() == GOVT_LIST_RESPONSE) {
                return processGovtListResponse(msg);
            } else {

                return toRespose("Unknown MessageType received", null, -1);
            }

        }

        return toRespose("Unknown MessageType received", null, -1);
    }

    private String eventLoop(Session session) throws InterruptedException {
        boolean done = false;
        while (!done) {
            Event event = session.nextEvent();
            if (event.eventType() == Event.EventType.PARTIAL_RESPONSE) {
                System.out.println("Processing Partial Response");
               return  processResponseEvent(event);
            } else if (event.eventType() == Event.EventType.RESPONSE) {
                System.out.println("Processing Response");
                return  processResponseEvent(event);
            } else {
                MessageIterator msgIter = event.messageIterator();
                while (msgIter.hasNext()) {
                    Message msg = msgIter.next();
                    System.out.println(msg.asElement());
                    if (event.eventType() == Event.EventType.SESSION_STATUS) {
                        if (msg.messageType() == SESSION_TERMINATED
                                || msg.messageType() == SESSION_FAILURE) {
                            done = true;
                        }
                    }
                }
            }
        }
        return toRespose("未知错误",null,-4);
    }

    private void sendRequest(Session session, Identity identity, String query, String maxResults) throws Exception {

        Service instrumentService = session.getService(INSTRUMENT_SERVICE);
        Request request;
        try {
            request = instrumentService.createRequest(d_requestType.toString());
        } catch (NotFoundException e) {
            throw new Exception(
                    String.format("Request type not found: %1$s", d_requestType),
                    e);
        }

        if(query!=null){
            d_queryString=query;
            request.set(QUERY_ELEMENT, d_queryString);
        }


        if(maxResults!=null){
            d_maxResults=Integer.parseInt(maxResults);
            request.set(MAX_RESULTS_ELEMENT, d_maxResults);
        }



        for (Entry<String, String> entry : d_filters.entrySet()) {
            try {
                request.set(entry.getKey(), entry.getValue());
            } catch (NotFoundException e) {
                throw new Exception(String.format("Filter not found: %1$s", entry.getKey()), e);
            } catch (InvalidConversionException e) {
                throw new Exception(
                        String.format(
                                "Invalid value: %1$s for filter: %2$s",
                                entry.getValue(),
                                entry.getKey()),
                        e);
            }
        }

        System.out.println(request);
        session.sendRequest(request, identity, null);
    }

    private static void stopSession(Session session) {
        if (session != null) {
            boolean done = false;
            while (!done) {
                try {
                    session.stop();
                    done = true;
                } catch (InterruptedException e) {
                    System.out.println("InterrupedException caught (ignoring)");
                }
            }
        }
    }

    private String run(String query, String maxResults) {
        Session session = null;
        try {
            // parseCommandLine(args);
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setServerHost(d_host);
            sessionOptions.setServerPort(d_port);
            sessionOptions.setAuthenticationOptions(d_authOptions);

            session = new Session(sessionOptions);
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

                System.err.println("Failed to start session.");
                return toRespose("Failed to start session.", null, -1);
            }

            Identity identity = session.createIdentity();
            if (d_authOptions != null) {
                authorize(identity, session);
            }

            if (!session.openService(INSTRUMENT_SERVICE)) {
                System.err.println("Failed to open " + INSTRUMENT_SERVICE);
                return toRespose("Failed to open " + INSTRUMENT_SERVICE, null, -2);
            }

            if(query!=null){
                d_queryString=query;

            }else{
                return toRespose("query不能为空",null,-7);
            }


            sendRequest(session, identity, query, maxResults);
          return   eventLoop(session);
        } catch (Exception e) {
            System.err.printf("Exception: %1$s\n", e.getMessage());
            return toRespose("Exception: "+e.getMessage(), null, -3);

        } finally {
            if (session != null) {
                stopSession(session);
            }
        }

    }


    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();

        String query = request.getParameter("query");
        String maxResults = request.getParameter("maxResults");

        out.println(run(query, maxResults));
    }


}
