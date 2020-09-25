package exerc2;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ResponseHandler implements Runnable {
    private static int buffer_size = 8192;

    private byte[] buffer;

    Socket socket = null;

    BufferedOutputStream ostream = null;

    BufferedInputStream istream = null;

    private StringBuffer header = null;

    private File root = null;

    static private String CRLF = "\r\n";

    public ResponseHandler(Socket socket, File root) {
        this.socket = socket;
        this.root = root;
    }

    public void init() throws IOException {

        this.buffer = new byte[buffer_size];
        this.header = new StringBuffer();
        this.istream = new BufferedInputStream(this.socket.getInputStream());//获取输入流
        this.ostream = new BufferedOutputStream(this.socket.getOutputStream()); //获取输出流

    }

    public void run() {

        try {

            init();//初始化

            processRequest();

            System.out.println("header:");
            System.out.println(header); //header包含客户端的请求信息

            processResponse();

            close();

        } catch (Exception e) {

            e.printStackTrace();
        }

    }


    public void processRequest() throws Exception {
        int last = 0, c = 0;
        /**
         * Process the header and add it to the header StringBuffer.
         * 对客户端请求进行解析，以报文最后有两个空格（CRLF = "\r\n";）为结束
         */
        boolean inHeader = true; // 对循环的控制
        while (inHeader && ((c = istream.read()) != -1)) {
            switch (c) {
                case '\r':
                    break;
                case '\n':
                    if (c == last) {
                        inHeader = false;
                        break;
                    }
                    last = c;
                    header.append("\n");
                    break;
                default:
                    last = c;
                    header.append((char) c);
            }
        }

    }


    //服务器端对请求信息进行相关处理
    public void processResponse() throws IOException {
        Scanner scanner = new Scanner(header.toString());
        String line = null;
        while ((line = scanner.next()) != null) {

            if (line.startsWith("GET")) {

                String filePath = scanner.next();
                if (filePath.equals("/")) {
                    filePath += "index.htm";
                }

                File file = new File(root.getAbsolutePath() + filePath);
                if (file.exists() && file.isFile()) {

                    String fileType = null;
                    String fileName = file.getName();

                    if (fileName.endsWith("htm")) {
                        fileType = "text/html";
                    } else if (fileName.endsWith("jpg")) {
                        fileType = "image/jpeg";
                    }
                    responseMes(fileType, "200 OK", file);

                } else {
                    responseMes("text/html", "404 Not Found");
                }
            } else if (line.startsWith("PUT")) {
                String filePath = scanner.next();

                File putFile = new File(root.getAbsolutePath() + filePath);
                if (putFile.exists()) {
                    if (!putFile.delete()) {
                        System.out.println("error:delete fail");
                    }
                }
                FileOutputStream outFile = new FileOutputStream(putFile);
                //解析文件，将文件内容写入到文件输出流，写进缓冲区，发送给客户段
                scanner.next();//跳过HTTP/1.0
                scanner.next();//跳过Content-Length:
                int fileLength = Integer.parseInt(scanner.next());
                for(int i = 0;i<fileLength/buffer_size+1;i++){
                    istream.read(buffer);
                    outFile.write(buffer);
                }


                outFile.close();
                responseMes("text/html", "201 Created");
            } else {
                responseMes("text/html", "501 Not Implemented");
            }
            break;
        }
    }
    //  然后将响应的报文信息返回给客户端  两个参数
    public void responseMes(String typeFile, String StatusCode) throws IOException {
        StringBuffer statusLine = new StringBuffer();
        String htmlFile = "<html><body><center><h1>" + StatusCode + "</h1></center></body></html>";

        statusLine.append("HTTP/1.0 ").append(StatusCode).append(CRLF);
        statusLine.append("Server: HttpServer/1.0").append(CRLF);
        statusLine.append("Content-type: ").append(typeFile).append(CRLF);
        statusLine.append("Content-length: ").append(htmlFile.length()).append(CRLF);
        statusLine.append(CRLF);
        statusLine.append(htmlFile);
        this.ostream.write(statusLine.toString().getBytes());
        this.ostream.flush();
    }

    //然后将响应的报文信息返回给客户端  三个参数
    public void responseMes(String typeFile, String StatusCode, File File) throws IOException {
        StringBuffer statusLine = new StringBuffer();

        statusLine.append("HTTP/1.0 ").append(StatusCode).append(CRLF);
        statusLine.append("Server: HttpServer/1.0").append(CRLF);
        statusLine.append("Content-type: ").append(typeFile).append(CRLF);
        statusLine.append("Content-length: ").append(File.length()).append(CRLF);
        statusLine.append(CRLF);
        this.ostream.write(statusLine.toString().getBytes());

        FileInputStream fileStream = new FileInputStream(File);
        while (fileStream.read(buffer) != -1) {
            this.ostream.write(buffer);
        }
        this.ostream.flush();
        fileStream.close();
    }

    public void close() throws Exception {
        this.socket.close();
        this.istream.close();
        this.ostream.close();
    }
}
