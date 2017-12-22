import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;

/**
 * YAE ASR Demo
 * 本Demo用Netty实现WebSocket连接，并返回相应的字符串
 */
public class YaeDemoClientOffline extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    /**
     * MD5字符
     */
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static void main(String[] args) throws Exception {

        //替换成用户实际的accessId
        String accessId = "";

        //替换成用户实际的accessKey
        String accessKey = "";

        //本次音频的request_id,替换成自己实际的取值
        String requestId = UUID.randomUUID().toString();

        //长度为10的随机字符串
        String nonceStr = randomString(10);

        //进行签名的字符串，生成方法是将所有参数按首字母顺序排序后拼接在一起，再在最后加上accessKey的值
        String signStr = "access_id=" + accessId
                + "&nonce_str=" + nonceStr
                + "&request_id=" + requestId
                + "&key=" + accessKey;

        //计算md5后的签名
        String sign = encode(signStr);

        //最终发送请求的地址串
        String url = "wss://asr.yuzhix.com/api/DecodeAudio?"
                + "access_id=" + accessId
                + "&nonce_str=" + nonceStr
                + "&request_id=" + requestId
                + "&sign=" + sign;


        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "wss" : uri.getScheme();

        //必须使用SSL连接，默认使用443端口
        final String host = uri.getHost();
        final int port = uri.getPort() == -1 ? 443 : uri.getPort();
        if (!"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WSS is supported.");
            return;
        }

        //SSL的初始化
        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //建立WebSocket连接
            final YaeDemoClientOffline handler =
                    new YaeDemoClientOffline(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()
                                            //是否为在线音频，预留参数，目前只支持true
                                            .set("online", false)));

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            Channel ch = b.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();
            ch.attr(AttributeKey.valueOf("response")).set(false);

            //从本地文件读取后转换成数据流识别
            String fileUri = YaeDemoClientOffline.class.getResource("/test.pcm").getFile();
            File file = new File(fileUri);

            FileInputStream fis = new FileInputStream(file);
            //每次发送6400字节的音频
            byte[] sendBytes = new byte[6400];

            int length;

            //在WebSocketClientHandler.java中接收回复并设置了response的值，如果该值为true则为已收到回复，不再继续发送音频
            while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                ByteBuf byteBuf = Unpooled.buffer(length);
                byteBuf.writeBytes(sendBytes, 0, length);
                ch.writeAndFlush(new BinaryWebSocketFrame(byteBuf));
            }

            //文件发送完毕后，发送一个包含任意内容的文本帧告知服务器发送完毕
            ch.writeAndFlush(new TextWebSocketFrame("finish!"));
            Thread.sleep(5000);
            //音频发完后五秒依然没有收到回复，直接发送关闭消息并退出连接
            if (!(Boolean) ch.attr(AttributeKey.valueOf("response")).get()) {
                ch.writeAndFlush(new CloseWebSocketFrame());

            }


        } finally {
            group.shutdownGracefully();
        }
    }

    public YaeDemoClientOffline(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                System.out.println("WebSocket Client connected!");
                handshakeFuture.setSuccess();
            } catch (WebSocketHandshakeException e) {
                System.out.println("WebSocket Client failed to connect");
                handshakeFuture.setFailure(e);
            }
            return;
        }

        //发生未知错误
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            System.out.println("WebSocket Client received message: " + textFrame.text());
        } else if (frame instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Client received closing");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }


    public static String encode(final String password) throws Exception {
        if (password == null) {
            return null;
        }
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(password.getBytes());
        final byte[] digest = messageDigest.digest();
        return getFormattedText(digest);
    }

    private static String getFormattedText(byte[] bytes) {
        final StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (int j = 0; j < bytes.length; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    public static String randomString(Integer length) {
        //将所有的大小写字母和0-9数字存入字符串中
        String str = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ0123456789";
        Random random = new Random();
        StringBuffer stringBuffer = new StringBuffer();
        for (int j = 0; j < length; j++) {
            //先随机生成初始定义的字符串 str 的某个索引，以获取相应的字符
            int index = random.nextInt(str.length());
            char c = str.charAt(index);
            stringBuffer.append(c);
        }

        return stringBuffer.toString();
    }

}
