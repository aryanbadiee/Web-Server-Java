/*
 * @author https://github.com/aryanbadiee
 */

import java.io.*;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.URL;

import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JavaServer implements Runnable{
    // for getting directory of this class:
    private static final URL location = JavaServer.class.getProtectionDomain().getCodeSource().getLocation();
	// removing name of file at the end:
    private static final String path = location.getFile().toLowerCase().replace("javaserver.java", "");
	
    private static final File WEB_ROOT = new File(path, "public");
    private static final String DEFAULT_FILE = "index.html";
    private static final String FILE_NOT_FOUND = "404.html";
    private static final String METHOD_NOT_SUPPORTED = "501.html";

    // port to listen connection
    private static final int PORT = 8080;

    // verbose mode
    private static final boolean verbose = true;

    // Client Connection via Socket Class
    private Socket connect;

    public JavaServer(Socket connect) {
        this.connect = connect;
    }

    public static void main(String[] args) {
        try {
			System.out.println(path);
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server Started.\nListening for connections on port : " + 
                PORT + " ...");
            System.out.println("--------------------------------------------");
            
            //Listen to Exit Server
            Exit myExit = new Exit();
            ExecutorService singleThread = Executors.newSingleThreadExecutor();   // single thread
            singleThread.execute(myExit);

            // We listen until the user stops the server
            while (true) {
                JavaServer myServer = new JavaServer(serverConnect.accept());
                if (verbose) {
                    System.out.println("Connection opened. (" + new Date() + ")");
                }
                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }
        }catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // we manage our particular client connection
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;
        try {
            //get binary input form client:
            InputStreamReader isReader = new InputStreamReader(connect.getInputStream());
            in = new BufferedReader(isReader);
            // get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());
            // get binary output stream to client (for body)
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // get first line of the request from the client
            String input = in.readLine();
            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            // we get the HTTP method of the client
            String method = parse.nextToken().toUpperCase();
            // we get file requested
            fileRequested = parse.nextToken().toLowerCase();

            // we support only GET and HEAD and POST methods, we check
            if (!method.equals("GET")  &&  !method.equals("HEAD") &&
                    !method.equals("POST")) {
                if (verbose) {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }
                // we return the not supported file to the client
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                //read content to return to client
                byte[] fileData = readFileData(file, fileLength);

                // we send HTTP Headers with data to client:
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: Java HTTP Server from Aryan Badiee : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer
                // Body:
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();

            }else {
                // GET or HEAD or POST method
                if (fileRequested.equals("/") ||
                        fileRequested.equals("/index")){
                    fileRequested = "/" + DEFAULT_FILE;
                }

                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);

                if (method.equals("GET") || method.equals("POST")) {

                    byte[] fileData = readFileData(file, fileLength);

                    // send HTTP Headers :
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server from Aryan Badiee : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer
                    // Body:
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }
                if (verbose) {
                    System.out.println("File " + fileRequested + " of type "
                            + content + " returned");
                }
            }
        }catch (FileNotFoundException fnfe) {
            try {
                fileNotFound(out, dataOut, fileRequested);
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : "
                        + ioe.getMessage());
            }
        }catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        }finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close();    // we close socket connection
            }catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }
            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }
    }

    // get data from body in HTTP:
    private String readBodyData(BufferedReader input) {
        String payload = "";
        try{
            while(input.readLine().length() != 0){
                // for skipping Header Request!
            }

            // code to read data from Body:
            while(input.ready()){
                payload += (char) input.read();
            }
        }catch(Exception ex){
            System.err.println(ex.getMessage());
        }
        return payload;
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        byte[] fileData = new byte[fileLength];
        try (FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(fileData);
        }
        return fileData;
    }

    // return supported MIME Types
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
            return "text/html";
        else if(fileRequested.endsWith(".css"))
            return "text/css";
        else if(fileRequested.endsWith(".js"))
            return "text/javascript";
        else
            return "text/plain";
    }

    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);
        // send HTTP Headers :
        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server from Aryan Badiee : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        // Body:
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }
}

class Exit implements Runnable {
    @Override
    public void run() {
        var scanner = new Scanner(System.in);
        while(true) {
            String myMessage = scanner.nextLine();
            if(myMessage.equals("$exit")){
                System.exit(1);     // for exit current process!
            }
        }
    }
}
