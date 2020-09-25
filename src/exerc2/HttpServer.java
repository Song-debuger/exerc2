package exerc2;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hansong
 *
 *
 */
public class HttpServer {

    ServerSocket server = null;

    private static final int PORT = 80;

    private File root = null;

    ExecutorService executorService; // 线程池

    final int POOL_SIZE = 4; // 单个处理器线程池工作线程数目

    public HttpServer(String root) throws IOException {
        this.root = new File(root);
        this.server = new ServerSocket(PORT, 5);
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * POOL_SIZE);
        // 创建线程池，根据可用处理器数目，设置线程数量
        System.out.println("server start...");
    }

    public static void main(String[] args) throws Exception {
        //验证是否传入参数
        if (args.length < 1) {
            System.out.println("路径参数不存在");
        } else if (isRight(args[0])) {
            // 验证文件夹路径是否正确
            new HttpServer(args[0]).service();
        } else {
            System.out.println("路径不存在或路径不是一个目录");
        }
    }

    public void service() throws Exception {
        Socket socket = null;
        //循环等待客户端连接
        while (true) {
            try {
                socket = server.accept(); // 等待并取出用户连接，并创建套接字

                executorService.execute(new ResponseHandler(socket, root));


            } // 如果客户端断开连接，则应捕获该异常，但不中断整个while循环，使得服务器能继续与其他客户端通信
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 验证path路径是否存在并且为文件夹
    private static boolean isRight(String path) {

        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }
}
