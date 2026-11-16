import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.net.*;

// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final int port = 8080;
    public static void main(String[] args) throws Exception {//TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("HTTP Server started on port " + port);


        while (true){

            Socket clientSocket = serverSocket.accept();

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    handleClient(clientSocket);
                }
            });



        }



    }

    public static void handleClient(Socket socket){

        try{

            HttpRequest request = readRequest(socket);

            if(request == null){
                return;
            }

            System.out.println(
                    request.method + " " + request.path
            );

            String responseBody;
            String contentType = "application/json";
            int statusCode = 200;
            String statusText = "OK";

            if(request.method.equals("GET") && request.path.equals("/")){
                responseBody = "Welcome to my custom HTTP server!";
                contentType = "text/plain";

            }
            else if(request.method.equals("GET") && request.path.equals("/hello")){
                responseBody = "{\"message\":\"Hello Kiran!\"}";
            }
            else if (request.method.equals("POST")
                    && request.path.equals("/echo")) {

                responseBody =
                        "{\"received\":\"" + escapeJson(request.body) + "\"}";

            }
            else {

                responseBody = "{\"error\":\"Route not found\"}";
                statusCode = 404;
                statusText = "Not Found";
            }

            sendResponse(
                    socket ,
                    statusCode,
                    statusText,
                    contentType,
                    responseBody
            );

        }
        catch (Exception e){
            System.out.println("Error: " + e.getMessage());

        }
        finally {
            try{
                socket.close();
            }
            catch (IOException e){
                System.out.println("Socket closing failed");

            }
        }

    }

    private static  HttpRequest readRequest(Socket socket) throws  IOException{

        InputStream input = socket.getInputStream();

        ByteArrayOutputStream headerBuffer =
                new ByteArrayOutputStream();

        int matched = 0;
        int value;

        while((value = input.read())!=-1){
            headerBuffer.write(value);

            if(matched==0 && value == '\r' ){
                matched = 1;
            }
            else if (matched == 1 && value == '\n') {
                matched = 2;
            } else if (matched == 2 && value == '\r') {
                matched = 3;
            } else if (matched == 3 && value == '\n') {
                break;
            } else if (value == '\r') {
                matched = 1;
            } else {
                matched = 0;
            }
        }


        if (value == -1) {
            return null;
        }
        String headersText =
                headerBuffer.toString(StandardCharsets.ISO_8859_1);

        String[] lines = headersText.split("\r\n");
        String[] requestParts = lines[0].split(" ", 3);

        if (requestParts.length != 3) {
            throw new IOException("Invalid HTTP request line");
        }

        String method = requestParts[0];
        String path = requestParts[1];

        int queryIndex = path.indexOf("?");

        if(queryIndex !=-1){
             path = path.substring(0 , queryIndex);
        }

        Map<String , String> headers = new HashMap<>();

        for(int i =1;i<lines.length;i++){
            int colonIndex = lines[i].indexOf(":");
            if (colonIndex > 0) {

                String key = lines[i]
                        .substring(0, colonIndex)
                        .trim()
                        .toLowerCase();

                String headerValue = lines[i]
                        .substring(colonIndex + 1)
                        .trim();

                headers.put(key, headerValue);
            }
        }

        int contentLength = 0;

        if (headers.containsKey("content-length")) {
            contentLength = Integer.parseInt(
                    headers.get("content-length")
            );
        }
        byte[] bodyBytes = new byte[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {

            int count = input.read(
                    bodyBytes,
                    totalRead,
                    contentLength - totalRead
            );

            if (count == -1) {
                throw new EOFException("Incomplete request body");
            }

            totalRead += count;
        }

        String body = new String(
                bodyBytes,
                StandardCharsets.UTF_8
        );


        return new HttpRequest(method, path, headers, body);




    }

    public static void sendResponse (
            Socket socket ,

            int statusCode,
            String statusText,
            String contentType,
            String body
    ) throws  IOException{
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        OutputStream output = socket.getOutputStream();
        String headers =
                "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                        + "Content-Type: " + contentType + "; charset=UTF-8\r\n"
                        + "Content-Length: " + bodyBytes.length + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n";

        output.write(headers.getBytes(StandardCharsets.UTF_8));

        output.write(bodyBytes);

        output.flush();

    } private static String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    static class HttpRequest {

        String method;
        String path;
        Map<String, String> headers;
        String body;

        HttpRequest(
                String method,
                String path,
                Map<String, String> headers,
                String body) {

            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }
    }

}